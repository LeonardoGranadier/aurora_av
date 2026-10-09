import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

public class SoundscapeTest {
    static void check(boolean value, String message) { VillageLifeTest.check(value, message); }
    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        Soundscape s = g.soundscape;
        check(s.desired().isEmpty(), "Interior has no outdoor sounds");
        g.map = g.maps.get("Praia Azul"); FestivalTest.date(g, 1, 10);
        check(s.desired().equals("mar_dia"), "Beach waves");
        FestivalTest.date(g, 1, 22); check(s.desired().equals("mar_noite"), "Night variant");
        g.weather.seed = Weather.LEGACY_SEED;
        FestivalTest.date(g, WeatherTest.dayOf(g.weather, Weather.RAIN), 10);
        check(s.desired().equals("mar_dia_chuva"), "Rain layered with waves");
        g.map = g.maps.get("Pico Estrelado"); check(s.desired().equals("vento_dia"), "Snow replaces rain");
        g.map = g.maps.get(ChapterFive.GARDEN); check(s.desired().equals("fonte_dia_chuva"), "Garden fountain with rain");
        g.state = Game.TITLE; check(s.desired().isEmpty(), "Title stops ambience");
        g.state = Game.PLAY;
        Set<Integer> hashes = new HashSet<>();
        for (String base : new String[]{"campo", "mar", "vento", "fonte"}) for (String period : new String[]{"dia", "noite"}) {
            byte[] data = Soundscape.render(base + "_" + period);
            check(data.length == 8 * 22050 * 2, "Eight-second loop");
            check(data[0] == 0 && data[1] == 0 && data[data.length - 1] == 0 && data[data.length - 2] == 0, "Loop boundaries silent");
            check(hashes.add(Arrays.hashCode(data)), "Distinct region and time sound");
            byte[] mute = Audio.scaled(data, 0);
            check(Arrays.equals(mute, new byte[data.length]), "Zero volume is exact silence");
            check(Arrays.equals(data, Audio.scaled(data, 100)), "Full volume preserves PCM");
            long energy = 0, halfEnergy = 0; int peak = 0;
            byte[] half = Audio.scaled(data, 50);
            for (int i = 0; i < data.length; i += 2) {
                int sample = (short) ((data[i] & 255) | (data[i + 1] << 8));
                int quieter = (short) ((half[i] & 255) | (half[i + 1] << 8));
                peak = Math.max(peak, Math.abs(sample));
                energy += (long) sample * sample; halfEnergy += (long) quieter * quieter;
            }
            check(peak > 100 && peak < 32767, "Audible PCM without clipping");
            check(halfEnergy > energy / 5 && halfEnergy <= energy / 4, "Half amplitude reduces energy to one quarter");
        }
        Properties bad = new Properties(); bad.setProperty("x", "oops"); check(Audio.volume(bad, "x", 65) == 65, "Invalid config fallback");
        bad.setProperty("x", "200"); check(Audio.volume(bad, "x", 65) == 100, "Config clamped");
        g.openPause(); g.choose("Volumes de áudio"); check(g.state == Game.SOUND, "Panel opens");
        VillageLifeTest.tap(g, KeyEvent.VK_LEFT); check(g.audio.musicVolume == 95 && g.audio.sfxVolume == 100, "Music independently adjusted");
        VillageLifeTest.tap(g, KeyEvent.VK_DOWN); VillageLifeTest.tap(g, KeyEvent.VK_LEFT);
        check(g.audio.sfxVolume == 95, "Effects adjusted");
        VillageLifeTest.tap(g, KeyEvent.VK_DOWN); VillageLifeTest.tap(g, KeyEvent.VK_LEFT);
        check(g.audio.ambientVolume == 60, "Ambience adjusted");
        g.audio.loadConfig(); check(g.audio.musicVolume == 95 && g.audio.sfxVolume == 95 && g.audio.ambientVolume == 60, "Settings persist");
        g.toastT = 0; VillageLifeTest.screenshot(g, "audio-volumes");
        VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.MENU, "Panel returns to pause");
        g.map = g.maps.get("Casa"); g.openPause(); VillageLifeTest.screenshot(g, "audio-pause");
        g.audio.setVolume(2, 0); g.audio.ambient("");
        g.audio.ambientWorker.submit(() -> {}).get(5, TimeUnit.SECONDS);
        check(g.audio.ambientClip == null, "Silent ambience releases clip");
        System.out.println("PASS: " + VillageLifeTest.checks + " soundscape checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE)) || Files.exists(Audio.CONFIG.toPath())) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
