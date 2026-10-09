import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Áudio do jogo. Música e efeitos são SINTETIZADOS por código (nenhum arquivo é necessário).
 * Para usar sua própria música/efeitos, coloque WAVs em assets/audio/ com os mesmos nomes
 * (ex.: vila_dia.wav, sfx_pickup.wav). Para gerar WAVs provisórios e editá-los:  java Audio
 * Sem placa de som disponível, o jogo simplesmente roda sem áudio.
 */
public class Audio {
    static final float RATE = 22050f;
    static final AudioFormat FMT = new AudioFormat(RATE, 16, 1, true, false);
    static final File DIR = new File("assets/audio"), CONFIG = new File("config.properties");
    static final float MUSIC_DB = -9f, SFX_DB = -4f;

    volatile boolean available, musicOn = true, sfxOn = true;
    volatile String wanted = "";
    volatile int musicVolume = 100, sfxVolume = 100, ambientVolume = 65;
    volatile String ambientWanted = "";
    String ambientPlaying;
    Clip ambientClip;
    final ExecutorService ambientWorker = daemon();
    String playing; Clip clip;
    final Map<String, byte[]> cache = new ConcurrentHashMap<>();
    final ExecutorService musicWorker = daemon(), sfxWorker = daemon();

    Audio() {
        loadConfig();
        musicWorker.submit(() -> {                      // detecta placa de som sem travar o jogo
            try { Clip c = AudioSystem.getClip(); c.close(); available = true; } catch (Throwable t) { available = false; }
            if (available) { switchMusic(wanted); ambientWorker.submit(() -> switchAmbient(ambientWanted)); }
        });
    }

    static ExecutorService daemon() {
        return Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "audio"); t.setDaemon(true); return t; });
    }

    // ---------- Opções ----------
    void loadConfig() {
        try (InputStream in = new FileInputStream(CONFIG)) {
            Properties p = new Properties(); p.load(in);
            musicOn = !"0".equals(p.getProperty("musica")); sfxOn = !"0".equals(p.getProperty("efeitos"));
            musicVolume = volume(p, "volume.musica", 100);
            sfxVolume = volume(p, "volume.efeitos", 100);
            ambientVolume = volume(p, "volume.ambiente", 65);
        } catch (Exception e) { /* sem config: padrão ligado */ }
    }
    void saveConfig() {
        try (OutputStream o = new FileOutputStream(CONFIG)) {
            Properties p = new Properties();
            p.setProperty("musica", musicOn ? "1" : "0"); p.setProperty("efeitos", sfxOn ? "1" : "0");
            p.setProperty("volume.musica", "" + musicVolume);
            p.setProperty("volume.efeitos", "" + sfxVolume);
            p.setProperty("volume.ambiente", "" + ambientVolume);
            p.store(o, "Aventura em Aurora - opcoes");
        } catch (Exception e) { }
    }
    void setMusic(boolean on) {
        musicOn = on; saveConfig();
        musicWorker.submit(() -> { if (!available) return; if (!on) stopMusic(true); else switchMusic(wanted); });
    }
    void setSfx(boolean on) { sfxOn = on; saveConfig(); }
    static int volume(Properties p, String key, int fallback) {
        try { return Math.max(0, Math.min(100, Integer.parseInt(p.getProperty(key, "" + fallback)))); }
        catch (NumberFormatException e) { return fallback; }
    }
    void setVolume(int channel, int value) {
        value = Math.max(0, Math.min(100, value));
        if (channel == 0) {
            musicVolume = value;
            musicWorker.submit(() -> { stopMusic(false); switchMusic(wanted); });
        } else if (channel == 1) sfxVolume = value;
        else {
            ambientVolume = value;
            ambientWorker.submit(() -> { stopAmbient(); switchAmbient(ambientWanted); });
        }
        saveConfig();
    }
    void ambient(String name) {
        if (name.equals(ambientWanted)) return;
        ambientWanted = name;
        ambientWorker.submit(() -> switchAmbient(name));
    }
    void stopAmbient() {
        if (ambientClip != null) { ambientClip.stop(); ambientClip.close(); ambientClip = null; }
        ambientPlaying = null;
    }
    void switchAmbient(String name) {
        if (!name.equals(ambientWanted)) return;
        if (name.equals(ambientPlaying)) return;
        stopAmbient();
        if (!available || name.isEmpty() || ambientVolume == 0) return;
        Clip c = null;
        try {
            byte[] data = scaled(cache.computeIfAbsent("amb_" + name, k -> Soundscape.render(name)), ambientVolume);
            if (!name.equals(ambientWanted)) return;
            c = AudioSystem.getClip(); c.open(FMT, data, 0, data.length);
            gain(c, -18); c.loop(Clip.LOOP_CONTINUOUSLY);
            ambientClip = c; ambientPlaying = name;
        } catch (Exception e) { if (c != null) c.close(); }
    }
    static byte[] scaled(byte[] data, int percent) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i += 2) {
            int value = (short) ((data[i] & 255) | (data[i + 1] << 8));
            value = value * percent / 100;
            result[i] = (byte) value; result[i + 1] = (byte) (value >> 8);
        }
        return result;
    }

    // ---------- Reprodução ----------
    void music(String name) { wanted = name; musicWorker.submit(() -> switchMusic(name)); }

    void switchMusic(String name) {
        if (!available || name.isEmpty() || !name.equals(wanted)) return;   // pedido antigo: ignora
        if (name.equals(playing) && clip != null) return;
        stopMusic(true);
        if (!musicOn || musicVolume == 0) return;
        try {
            Clip c = open(name, false);
            gain(c, -50);
            c.loop(Clip.LOOP_CONTINUOUSLY);
            clip = c; playing = name;
            fade(c, -50, MUSIC_DB);
        } catch (Throwable t) { System.err.println("Áudio: não foi possível tocar '" + name + "': " + t); }
    }

    void stopMusic(boolean withFade) {
        if (clip == null) return;
        try { if (withFade) fade(clip, MUSIC_DB, -50); } catch (InterruptedException ignored) { }
        clip.stop(); clip.close(); clip = null; playing = null;
    }

    void sfx(String name) {
        if (!sfxOn || sfxVolume == 0) return;
        sfxWorker.submit(() -> {
            if (!available || !sfxOn || sfxVolume == 0) return;
            try {
                Clip c = open(name, true);
                gain(c, SFX_DB);
                c.addLineListener(e -> { if (e.getType() == LineEvent.Type.STOP) c.close(); });
                c.start();
            } catch (Throwable t) { /* efeito perdido: sem problema */ }
        });
    }

    Clip open(String name, boolean effect) throws Exception {
        File f = new File(DIR, (effect ? "sfx_" : "") + name + ".wav");
        Clip c = AudioSystem.getClip();
        try {
        if (f.exists()) {
            try (AudioInputStream source = AudioSystem.getAudioInputStream(f);
                 AudioInputStream in = AudioSystem.getAudioInputStream(FMT, source)) {
                byte[] d = scaled(in.readAllBytes(), effect ? sfxVolume : musicVolume);
                c.open(FMT, d, 0, d.length);
            }
        } else {
            byte[] d = cache.computeIfAbsent((effect ? "sfx_" : "") + name, k -> effect ? renderSfx(name) : renderTrack(name));
            d = scaled(d, effect ? sfxVolume : musicVolume);
            c.open(FMT, d, 0, d.length);
        }
        return c;
        } catch (Exception e) { c.close(); throw e; }
    }

    static void gain(Clip c, float db) {
        if (!c.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl fc = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
        fc.setValue(Math.max(fc.getMinimum(), Math.min(fc.getMaximum(), db)));
    }
    static void fade(Clip c, float from, float to) throws InterruptedException {
        for (int i = 0; i <= 8; i++) { gain(c, from + (to - from) * i / 8f); Thread.sleep(25); }
    }

    // =====================================================================
    //  SÍNTESE
    // =====================================================================
    static final int[] MAJOR = {0, 2, 4, 5, 7, 9, 11}, MINOR = {0, 2, 3, 5, 7, 8, 10},
                       DORIAN = {0, 2, 3, 5, 7, 9, 10}, LYDIAN = {0, 2, 4, 6, 7, 9, 11};

    /** lead: 0 = triângulo, 1 = pulso, 2 = sino | perc: 0 nenhuma, 1 chimbal, 2 chimbal+bumbo+caixa | bright: 1 = brilhante, baixo = abafado */
    record Theme(int root, int[] scale, int bpm, int[] prog, int lead, String arp, int perc,
                 double rest, double leadVol, double bright, boolean pad, boolean waves, long seed) {}

    static final Map<String, Theme> THEMES = new LinkedHashMap<>();
    static void theme(String n, int root, int[] sc, int bpm, int[] pr, int lead, String arp, int perc,
                      double rest, double lv, double br, boolean pad, boolean waves, long seed) {
        THEMES.put(n, new Theme(root, sc, bpm, pr, lead, arp, perc, rest, lv, br, pad, waves, seed));
    }
    static {
        //     nome              raiz escala  bpm  progressão (graus)       lead arp         perc pausas vol   brilho pad    ondas  seed
        theme("titulo",          60, LYDIAN,  84, new int[]{0, 4, 5, 3}, 2, "01210120", 0, 0.12, 0.30, 0.70, true,  false, 1);
        theme("casa",            65, MAJOR,   92, new int[]{0, 3, 4, 0}, 0, "0.1.2.1.", 0, 0.20, 0.26, 0.55, false, false, 2);
        theme("vila_dia",        67, MAJOR,  116, new int[]{0, 4, 5, 3}, 1, "01210121", 2, 0.10, 0.20, 0.55, false, false, 3);
        theme("vila_noite",      69, MINOR,   72, new int[]{0, 5, 3, 6}, 2, "0.1.2.1.", 0, 0.35, 0.30, 0.40, true,  false, 4);
        theme("floresta_dia",    62, DORIAN, 100, new int[]{0, 3, 0, 4}, 0, "0.12.210", 1, 0.20, 0.28, 0.60, false, false, 5);
        theme("floresta_noite",  62, MINOR,   64, new int[]{0, 5, 3, 4}, 2, "0...1...", 0, 0.40, 0.30, 0.35, true,  false, 6);
        theme("praia_dia",       60, MAJOR,  120, new int[]{0, 3, 4, 3}, 1, "01201210", 2, 0.12, 0.20, 0.55, false, true,  7);
        theme("praia_noite",     64, MINOR,   66, new int[]{0, 2, 3, 4}, 2, "0.1.2.1.", 0, 0.35, 0.30, 0.40, true,  true,  8);
        theme("ilha_dia",        69, LYDIAN, 108, new int[]{0, 2, 4, 3}, 1, "01210121", 1, 0.12, 0.20, 0.55, false, true,  9);
        theme("ilha_noite",      69, MINOR,   70, new int[]{0, 6, 5, 4}, 2, "0.1.2...", 0, 0.38, 0.30, 0.38, true,  true, 10);
        theme("gruta",           60, MINOR,   54, new int[]{0, 5, 0, 6}, 2, "0.......", 0, 0.50, 0.28, 0.30, true,  false, 11);
        theme("templo",          64, DORIAN,  60, new int[]{0, 4, 5, 3}, 2, "0.2.1.2.", 0, 0.45, 0.28, 0.35, true,  false, 12);
        theme("final",           60, MAJOR,   80, new int[]{0, 4, 5, 3}, 2, "01210121", 0, 0.12, 0.32, 0.60, true,  true,  13);
        theme("montanha_dia",    62, MAJOR,   78, new int[]{0, 5, 3, 4}, 2, "0.1.2.1.", 0, 0.30, 0.30, 0.50, true,  false, 14);
        theme("montanha_noite",  62, MINOR,   58, new int[]{0, 5, 3, 6}, 2, "0...1...", 0, 0.42, 0.30, 0.35, true,  false, 15);
        theme("cabana",          67, DORIAN,  88, new int[]{0, 3, 4, 0}, 0, "0.12.1.2", 0, 0.18, 0.26, 0.55, false, false, 16);
        theme("pico",            64, LYDIAN,  66, new int[]{0, 4, 2, 3}, 2, "0.2.1.2.", 0, 0.30, 0.30, 0.45, true,  false, 17);
        theme("jardim",          65, LYDIAN,  74, new int[]{0, 3, 5, 4}, 2, "0.21.210", 0, 0.22, 0.27, 0.48, true,  false, 18);
    }
    static final String[] SFX = {"blip", "select", "confirm", "back", "pickup", "door", "bad", "fire",
                                 "fanfare", "save", "sleep", "step", "warp", "slide", "twinkle", "bell0", "bell1", "bell2", "bell3"};

    static double freq(double midi) { return 440 * Math.pow(2, (midi - 69) / 12.0); }
    static int deg(Theme th, int d) { return th.root() + 12 * Math.floorDiv(d, 7) + th.scale()[Math.floorMod(d, 7)]; }

    static double wave(int type, double ph) {
        double x = ph - Math.floor(ph);
        switch (type) {
            case 0: return 4 * Math.abs(x - 0.5) - 1;          // triângulo
            case 1: return (x < 0.25 ? 1 : -1) + 0.5;           // pulso 25% (+0.5 remove o deslocamento DC)
            case 2: return Math.sin(2 * Math.PI * x);           // seno
            case 3: return x < 0.5 ? 1 : -1;                    // quadrada
            default: return 2 * x - 1;                          // dente de serra
        }
    }

    /** Soma uma nota ao buffer. A nota sempre termina (com release) antes do fim do buffer, para o loop não dar estalo. */
    static void note(float[] buf, double start, double dur, double f, double vol, int type, double decay, double attack, double release) {
        double total = buf.length / (double) RATE;
        dur = Math.min(dur, total - start - release);
        if (dur <= 0) return;
        int s0 = (int) (start * RATE), n = (int) ((dur + release) * RATE);
        double ph = 0, inc = f / RATE;
        for (int i = 0; i < n; i++) {
            int idx = s0 + i;
            if (idx >= buf.length) break;
            double t = i / (double) RATE;
            double env = Math.min(1, t / attack) * Math.exp(-decay * t);
            if (t > dur) env *= Math.max(0, 1 - (t - dur) / release);
            buf[idx] += (float) (vol * env * wave(type, ph));
            ph += inc;
        }
    }

    static void sweep(float[] buf, double start, double dur, double f0, double f1, double vol, int type, double decay) {
        int s0 = (int) (start * RATE), n = (int) (dur * RATE);
        double ph = 0;
        for (int i = 0; i < n; i++) {
            int idx = s0 + i;
            if (idx >= buf.length) break;
            double t = i / (double) RATE, f = f0 + (f1 - f0) * (i / (double) n);
            double env = Math.min(1, t / 0.005) * Math.exp(-decay * t) * Math.min(1, (dur - t) / 0.02);
            buf[idx] += (float) (vol * env * wave(type, ph));
            ph += f / RATE;
        }
    }

    static double noiseGain(double a) { return 0.4 / (0.577 * Math.sqrt(a / (2 - a))); }

    /** Ruído filtrado (a pequeno = grave/suave, a = 1 = branco). */
    static void noise(float[] buf, double start, double dur, double vol, double decay, double a, long seed) {
        Random r = new Random(seed);
        int s0 = (int) (start * RATE), n = (int) (dur * RATE);
        float lp = 0;
        double g = noiseGain(a);
        for (int i = 0; i < n; i++) {
            int idx = s0 + i;
            if (idx >= buf.length) break;
            lp += (float) (a * ((r.nextFloat() * 2 - 1) - lp));
            double t = i / (double) RATE;
            double env = Math.min(1, t / 0.004) * Math.exp(-decay * t) * Math.min(1, (dur - t) / 0.01);
            buf[idx] += (float) (vol * env * lp * g);
        }
    }

    // ---------- Composição ----------
    static final double[][] R = {{1, 1, 1, 1}, {2, 1, 1}, {1.5, 0.5, 1, 1}, {1, 1, 2},
                                 {0.5, 0.5, 1, 2}, {2, 2}, {1, 0.5, 0.5, 1, 1}, {1, 1, 0.5, 0.5, 1}};

    static int nearest(int[] tones, int target) {
        int best = tones[0] + 7, bd = 99;
        for (int t : tones) for (int o = -7; o <= 14; o += 7) {
            int c = t + o;
            if (c < 3 || c > 11) continue;
            int dd = Math.abs(c - target);
            if (dd < bd) { bd = dd; best = c; }
        }
        return best;
    }

    /** Um compasso de melodia: {início em tempos, duração em tempos, grau (-999 = pausa)}. */
    static double[][] genBar(Random r, int chordDeg, double[][] pool, double rest, int[] prev, boolean cadence) {
        double[] rh = pool[r.nextInt(pool.length)];
        double[][] out = new double[rh.length][];
        int[] tones = {chordDeg, chordDeg + 2, chordDeg + 4};
        int[] steps = {-2, -1, -1, 0, 1, 1, 2};
        double off = 0;
        for (int i = 0; i < rh.length; i++) {
            int d;
            if (i == 0) d = nearest(tones, prev[0]);                       // começa num tom do acorde
            else {
                d = Math.max(3, Math.min(11, prev[0] + steps[r.nextInt(steps.length)]));
                if (rh[i] >= 2) d = nearest(tones, d);                     // nota longa pousa num tom do acorde
            }
            boolean silent = i > 0 && r.nextDouble() < rest;
            double dur = rh[i];
            if (cadence && i == rh.length - 1) { d = 7; silent = false; dur = Math.max(dur, 2); }   // termina na tônica
            out[i] = new double[]{off, dur, silent ? -999 : d};
            if (!silent) prev[0] = d;
            off += rh[i];
        }
        return out;
    }

    static float[] render(Theme th) {
        double beat = 60.0 / th.bpm(), bar = beat * 4;
        int nb = th.prog().length * 2;
        float[] buf = new float[(int) (nb * bar * RATE)];
        Random r = new Random(th.seed());

        for (int b = 0; b < nb; b++) {
            int d = th.prog()[b % th.prog().length];
            double t0 = b * bar, step = beat / 2;
            // baixo
            double bf = freq(deg(th, d - 14));
            if (th.pad()) note(buf, t0, bar * 0.98, bf, 0.30, 0, 0.3, 0.02, 0.15);
            else { note(buf, t0, beat * 1.9, bf, 0.32, 0, 1.0, 0.01, 0.08); note(buf, t0 + 2 * beat, beat * 1.9, bf, 0.28, 0, 1.0, 0.01, 0.08); }
            // colchão de acordes
            if (th.pad()) for (int tone : new int[]{d, d + 2, d + 4}) note(buf, t0, bar, freq(deg(th, tone - 7)), 0.05, 2, 0, 0.6, 0.6);
            // arpejo
            for (int k = 0; k < 8; k++) {
                char ch = th.arp().charAt(k % th.arp().length());
                if (ch == '.') continue;
                int dd = ch == '0' ? d : ch == '1' ? d + 2 : ch == '2' ? d + 4 : d + 7;
                note(buf, t0 + k * step, step * 0.9, freq(deg(th, dd)), 0.10, th.lead() == 2 ? 2 : 1, 6.0, 0.004, 0.03);
            }
            // percussão
            if (th.perc() >= 1) for (int k = 1; k < 8; k += 2) noise(buf, t0 + k * step, 0.03, 0.05, 90, 1.0, r.nextLong());
            if (th.perc() >= 2) {
                for (int i : new int[]{0, 2}) note(buf, t0 + i * beat, 0.12, 62, 0.20, 2, 18, 0.002, 0.02);
                for (int i : new int[]{1, 3}) noise(buf, t0 + i * beat, 0.09, 0.07, 30, 0.7, r.nextLong());
            }
        }

        // melodia: frase A (metade) repetida, com uma cadência nova no último compasso
        double[][][] mel = new double[nb][][];
        int[] prev = {7};
        double[][] pool = th.rest() >= 0.3 ? new double[][]{R[1], R[3], R[5], R[2]} : R;
        int half = nb / 2;
        for (int b = 0; b < half; b++) mel[b] = genBar(r, th.prog()[b % th.prog().length], pool, th.rest(), prev, false);
        for (int b = half; b < nb - 1; b++) mel[b] = mel[b - half];
        mel[nb - 1] = genBar(r, th.prog()[(nb - 1) % th.prog().length], pool, th.rest() * 0.5, prev, true);
        for (int b = 0; b < nb; b++) for (double[] nt : mel[b]) {
            if (nt[2] < -900) continue;
            double st = b * bar + nt[0] * beat, du = nt[1] * beat * 0.95, f = freq(deg(th, (int) nt[2]));
            switch (th.lead()) {
                case 0 -> note(buf, st, du, f, th.leadVol(), 0, 2.0, 0.015, 0.06);
                case 1 -> note(buf, st, du, f, th.leadVol(), 1, 3.0, 0.008, 0.05);
                default -> { note(buf, st, du, f, th.leadVol(), 2, 1.6, 0.01, 0.2); note(buf, st, du, f * 2, th.leadVol() * 0.3, 2, 3.5, 0.01, 0.2); }
            }
        }

        // Environmental sounds are mixed independently by Soundscape.
        return finish(buf, th.bright());
    }

    /** Eco e filtro CIRCULARES (a cauda volta ao início), então o loop emenda sem estalo. */
    static float[] finish(float[] x, double bright) {
        int n = x.length, d = (int) (0.19 * RATE);
        float[] y = new float[n];
        for (int i = 0; i < n; i++) y[i] = x[i] + 0.26f * x[(i - d + n) % n] + 0.13f * x[((i - 2 * d) % n + n) % n];
        float a = (float) bright, lp = 0;
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < n; i++) { lp += a * (y[i] - lp); if (pass == 1) y[i] = lp; }
        double mean = 0;                                  // remove qualquer DC restante
        for (float v : y) mean += v;
        mean /= n;
        for (int i = 0; i < n; i++) y[i] -= (float) mean;
        float peak = 0;
        for (float v : y) peak = Math.max(peak, Math.abs(v));
        if (peak > 0) for (int i = 0; i < n; i++) y[i] *= 0.85f / peak;
        return y;
    }

    static byte[] renderTrack(String name) {
        Theme th = THEMES.get(name);
        if (th == null) th = THEMES.get("vila_dia");
        return toBytes(render(th));
    }

    // ---------- Efeitos sonoros ----------
    static byte[] renderSfx(String name) {
        float[] b;
        switch (name) {
            case "bell0", "bell1", "bell2", "bell3" -> {
                b = new float[(int) (0.55 * RATE)];
                int midi = new int[]{72, 74, 76, 79}[name.charAt(4) - '0'];
                note(b, 0, 0.30, freq(midi), 0.38, 2, 5, 0.003, 0.18);
                note(b, 0, 0.24, freq(midi + 12), 0.12, 2, 8, 0.003, 0.14);
            }
            case "blip" -> { b = new float[(int) (0.09 * RATE)]; note(b, 0, 0.05, 880, 0.30, 3, 30, 0.002, 0.01); note(b, 0.03, 0.05, 1320, 0.18, 3, 30, 0.002, 0.01); }
            case "select" -> { b = new float[(int) (0.08 * RATE)]; note(b, 0, 0.04, 660, 0.30, 0, 25, 0.002, 0.01); }
            case "confirm" -> { b = new float[(int) (0.24 * RATE)]; note(b, 0, 0.07, 784, 0.28, 1, 18, 0.002, 0.01); note(b, 0.07, 0.12, 1175, 0.28, 1, 14, 0.002, 0.02); }
            case "back" -> { b = new float[(int) (0.20 * RATE)]; note(b, 0, 0.06, 523, 0.28, 0, 18, 0.002, 0.01); note(b, 0.06, 0.09, 392, 0.28, 0, 18, 0.002, 0.02); }
            case "pickup" -> {
                b = new float[(int) (0.7 * RATE)];
                int[] m = {72, 76, 79, 84};
                for (int i = 0; i < 4; i++) note(b, i * 0.07, 0.08, freq(m[i]), 0.26, 1, 10, 0.002, 0.02);
                note(b, 0.21, 0.35, freq(88), 0.12, 2, 6, 0.002, 0.1);
            }
            case "door" -> { b = new float[(int) (0.45 * RATE)]; noise(b, 0, 0.35, 0.30, 9, 0.12, 7); note(b, 0, 0.22, 110, 0.35, 2, 12, 0.003, 0.05); }
            case "bad" -> { b = new float[(int) (0.5 * RATE)]; sweep(b, 0, 0.38, 220, 150, 0.22, 1, 3); sweep(b, 0, 0.38, 233, 157, 0.22, 1, 3); }
            case "fire" -> { b = new float[(int) (0.65 * RATE)]; noise(b, 0, 0.5, 0.28, 4, 0.5, 11); sweep(b, 0, 0.5, 260, 880, 0.16, 0, 3); }
            case "fanfare" -> {
                b = new float[(int) (1.5 * RATE)];
                int[] m = {72, 76, 79};
                for (int i = 0; i < 3; i++) note(b, i * 0.14, 0.12, freq(m[i]), 0.24, 1, 4, 0.004, 0.03);
                note(b, 0.42, 0.9, freq(84), 0.24, 1, 2.5, 0.004, 0.2);
                note(b, 0.42, 0.9, freq(88), 0.14, 0, 2.5, 0.004, 0.2);
                note(b, 0.42, 0.9, freq(60), 0.22, 0, 1.5, 0.004, 0.2);
            }
            case "save" -> { b = new float[(int) (0.55 * RATE)]; note(b, 0, 0.15, freq(81), 0.28, 2, 9, 0.003, 0.05); note(b, 0.12, 0.3, freq(88), 0.28, 2, 7, 0.003, 0.1); note(b, 0.12, 0.3, freq(100), 0.08, 2, 12, 0.003, 0.1); }
            case "sleep" -> {
                b = new float[(int) (1.7 * RATE)];
                int[] m = {79, 76, 72, 67};
                for (int i = 0; i < 4; i++) note(b, i * 0.35, 0.5, freq(m[i]), 0.24, 2, 3, 0.01, 0.15);
            }
            case "step" -> { b = new float[(int) (0.06 * RATE)]; noise(b, 0, 0.045, 0.16, 55, 0.25, 3); }
            case "warp" -> { b = new float[(int) (1.0 * RATE)]; sweep(b, 0, 0.9, 180, 700, 0.20, 2, 2); noise(b, 0, 0.9, 0.12, 2, 0.08, 5); }
            case "slide" -> { b = new float[(int) (0.36 * RATE)]; noise(b, 0, 0.3, 0.24, 6, 0.30, 21); sweep(b, 0, 0.3, 520, 300, 0.06, 2, 5); }
            case "twinkle" -> {
                b = new float[(int) (0.7 * RATE)];
                int[] m = {96, 100, 103, 108};
                for (int i = 0; i < 4; i++) note(b, i * 0.08, 0.1, freq(m[i]), 0.18, 2, 8, 0.002, 0.08);
            }
            default -> b = new float[(int) (0.05 * RATE)];
        }
        return toBytes(b);
    }

    static byte[] toBytes(float[] x) {
        byte[] b = new byte[x.length * 2];
        for (int i = 0; i < x.length; i++) {
            int v = Math.round(Math.max(-1f, Math.min(1f, x[i])) * 32767);
            b[2 * i] = (byte) v; b[2 * i + 1] = (byte) (v >> 8);
        }
        return b;
    }

    static void writeWav(File f, byte[] d) throws IOException {
        try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(d), FMT, d.length / 2)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, f);
        }
    }

    /** java Audio  -> grava todas as músicas e efeitos como WAV em assets/audio/ (para ouvir fora do jogo ou editar). */
    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        for (String n : THEMES.keySet()) { writeWav(new File(DIR, n + ".wav"), renderTrack(n)); System.out.println(n + ".wav"); }
        for (String n : SFX) { writeWav(new File(DIR, "sfx_" + n + ".wav"), renderSfx(n)); System.out.println("sfx_" + n + ".wav"); }
        System.out.println("WAVs gravados em " + DIR + " (o jogo passa a usá-los no lugar dos sintetizados).");
    }
}
