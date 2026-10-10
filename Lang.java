import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Idiomas. Os textos do jogo continuam escritos em português no código; ao desenhar, cada texto passa por t(),
 * que procura a tradução no dicionário assets/lang/<idioma>.tsv (português TAB tradução, uma linha por texto).
 *
 * Textos montados com números ou nomes ("Conchas: 3/12", "Presente para Tio Beto") usam moldes: os números viram
 * {0}, {1}... e os nomes do glossário (moradores, lugares, peixes, itens...) viram {a}, {b}... na ordem em que
 * aparecem; o molde é traduzido e os pedaços voltam traduzidos. Sem tradução, o texto aparece em português.
 */
final class Lang {
    private Lang() {}

    static final String[] CODES = {"pt", "en"};
    static final String[] LABELS = {"Português", "English"};
    static String current = "pt";

    private static final Map<String, String> dictionary = new HashMap<>();
    private static final List<String> names = new ArrayList<>();
    private static final Map<String, String> cache = new HashMap<>();
    static List<String> names() { return Collections.unmodifiableList(names); }
    /** Quantos textos diferentes já passaram pelo tradutor (para os testes conferirem a cobertura). */
    static int translatedCount() { return cache.size(); }
    private static final Set<String> done = new HashSet<>();
    /** Textos sem tradução encontrados nesta sessão (para revisar o dicionário). */
    static final Set<String> missing = new TreeSet<>();
    private static Pattern namePattern;
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern LETTER = Pattern.compile("\\p{L}{2,}");

    static boolean english() { return current.equals("en"); }
    static int index() { return Math.max(0, Arrays.asList(CODES).indexOf(current)); }

    /**
     * Idioma inicial: o escolhido antes (idioma.properties), senão -Daurora.idioma, senão o do sistema
     * (português para pt, inglês para os demais). Sem tela (testes), o padrão é português.
     */
    static void init() {
        String code = null;
        Properties p = new Properties();
        try (var in = Files.newInputStream(GameFiles.data("idioma.properties"))) { p.load(in); code = p.getProperty("idioma"); } catch (Exception ignored) {}
        if (code == null) code = System.getProperty("aurora.idioma");
        if (code == null) code = java.awt.GraphicsEnvironment.isHeadless() || Locale.getDefault().getLanguage().equals("pt") ? "pt" : "en";
        set(code);
        String report = System.getProperty("aurora.idioma.faltando");
        if (report != null) Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { Files.write(new File(report).toPath(), missing, StandardCharsets.UTF_8); } catch (Exception ignored) {}
        }));
    }
    /** Próximo idioma da lista, guardado para as próximas vezes. */
    static void cycle() {
        set(CODES[(index() + 1) % CODES.length]);
        Properties p = new Properties(); p.setProperty("idioma", current);
        try (var out = Files.newOutputStream(GameFiles.data("idioma.properties"))) { p.store(out, "Aventura em Aurora - idioma"); } catch (Exception ignored) {}
    }

    /** Troca o idioma e carrega o dicionário. */
    static void set(String code) {
        current = Arrays.asList(CODES).contains(code) ? code : "pt";
        dictionary.clear(); cache.clear(); done.clear();
        if (current.equals("pt")) return;
        File file = GameFiles.asset("lang/" + current + ".tsv");
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                int tab = line.indexOf('\t');
                if (tab > 0) dictionary.put(unescape(line.substring(0, tab)), unescape(line.substring(tab + 1)));
            }
        } catch (Exception e) { System.err.println("Dicionário de idioma não encontrado: " + file); }
    }
    static String unescape(String s) { return s.replace("\\n", "\n").replace("\\t", "\t"); }

    /** Nomes próprios e termos que aparecem dentro de textos montados (moldes com {a}, {b}...). */
    private static boolean initialized;
    static void initOnce() { if (!initialized) { initialized = true; init(); } }

    static void addNames(String... list) {
        for (String n : list) if (n != null && !n.isBlank() && !names.contains(n)) names.add(n);
        names.sort(Comparator.comparingInt(String::length).reversed());
        StringBuilder b = new StringBuilder();
        for (String n : names) b.append(b.length() == 0 ? "" : "|").append(Pattern.quote(n));
        namePattern = Pattern.compile("(?<![\\p{L}])(" + b + ")(?![\\p{L}])");
        cache.clear();
    }

    /** Tradução de um texto para o idioma atual (em português, devolve o próprio texto). */
    static String t(String s) {
        if (current.equals("pt") || s == null || s.isEmpty() || done.contains(s)) return s;
        String cached = cache.get(s);
        if (cached != null) return cached;
        String result = translate(s);
        if (cache.size() > 20000) cache.clear();
        cache.put(s, result);
        return result;
    }

    /** Marca um texto montado a partir de partes já traduzidas e o devolve. */
    static String done(String s) { markDone(List.of(s)); return s; }

    /** Linhas já traduzidas (por exemplo, quebradas por Game.wrap) não são traduzidas de novo. */
    static void markDone(Collection<String> lines) {
        if (current.equals("pt")) return;
        if (done.size() > 20000) done.clear();
        done.addAll(lines);
    }

    private static String translate(String s) {
        String exact = dictionary.get(s);
        if (exact != null) return exact;
        // símbolos no início ("▶ ", "● ", "> ", "[x] ") e espaços nas pontas ficam como estão
        int start = 0, end = s.length();
        if (s.startsWith("[x] ")) start = 4;
        while (start < end && !Character.isLetterOrDigit(s.charAt(start)) && s.charAt(start) != '"' && s.charAt(start) != '¿') start++;
        while (end > start && Character.isWhitespace(s.charAt(end - 1))) end--;
        if (start > 0 || end < s.length()) {
            String core = s.substring(start, end);
            String inner = translateCore(core);
            if (inner != null) return s.substring(0, start) + inner + s.substring(end);
        }
        String inner = translateCore(s);
        if (inner != null) return inner;
        if (LETTER.matcher(s).find()) synchronized (missing) { missing.add(s); }
        return s;
    }

    private static String translateCore(String s) {
        String exact = dictionary.get(s);
        if (exact != null) return exact;
        // molde: números → {0}, {1}...; nomes do glossário → {a}, {b}...
        List<String> numbers = new ArrayList<>(), parts = new ArrayList<>();
        Matcher m = NUMBER.matcher(s);
        StringBuilder masked = new StringBuilder();
        while (m.find()) { m.appendReplacement(masked, "{" + numbers.size() + "}"); numbers.add(m.group()); }
        m.appendTail(masked);
        String key = masked.toString();
        if (!numbers.isEmpty()) {                                     // molde só com números (nomes escritos por extenso)
            String template = dictionary.get(key);
            if (template != null) {
                for (int i = 0; i < numbers.size(); i++) template = template.replace("{" + i + "}", numbers.get(i));
                return template;
            }
        }
        if (namePattern != null) {
            Matcher n = namePattern.matcher(key);
            StringBuilder named = new StringBuilder();
            while (n.find()) {
                char tag = (char) ('a' + parts.size());
                n.appendReplacement(named, Matcher.quoteReplacement("{" + tag + "}")); parts.add(n.group(1));
            }
            n.appendTail(named);
            key = named.toString();
        }
        if (numbers.isEmpty() && parts.isEmpty()) return null;
        String template = dictionary.get(key);
        if (template == null) return null;
        String out = template;
        for (int i = 0; i < numbers.size(); i++) out = out.replace("{" + i + "}", numbers.get(i));
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i), tr = dictionary.getOrDefault(part, part);
            out = out.replace("{" + (char) ('a' + i) + "}", tr);
        }
        return out;
    }
}
