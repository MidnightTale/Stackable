package net.hynse.stackable.util;

import net.hynse.stackable.Stackable;
import org.bukkit.Material;

import java.util.Map;
import java.util.logging.Level;

/**
 * Glob-style wildcard matching for material names (*boat, *_shulker_box, music_disc_*, etc.).
 */
public final class RegexUtil {
    private static final char WILDCARD = '*';

    private RegexUtil() {}

    /**
     * Returns the best (maximum) stack size among all matching patterns, or {@code def} if none match.
     */
    public static int getBestMatchOrDefault(Material material, Map<String, Integer> patterns, int def) {
        if (patterns == null || patterns.isEmpty()) {
            return def;
        }

        String materialName = material.name().toUpperCase();
        int best = -1;

        for (Map.Entry<String, Integer> entry : patterns.entrySet()) {
            String pattern = entry.getKey().toUpperCase();
            if (!matchWildcard(pattern, materialName)) {
                continue;
            }
            best = Math.max(best, entry.getValue());
            logDebug("Pattern '" + pattern + "' matches " + materialName + " -> " + entry.getValue());
        }

        return best >= 0 ? best : def;
    }

    /**
     * @deprecated use {@link #getBestMatchOrDefault(Material, Map, int)}
     */
    @Deprecated
    public static int getMatchOrDefault(Material material, Map<String, Integer> patterns, int def) {
        return getBestMatchOrDefault(material, patterns, def);
    }

    /** No-op kept for callers that cleared the old value cache. */
    public static void clearCache() {
        // Matching is cheap and must not cache values across different group maps.
    }

    public static boolean testPattern(String pattern, String materialName) {
        boolean matches = matchWildcard(pattern.toUpperCase(), materialName.toUpperCase());
        logDebug("Testing pattern '" + pattern + "' against '" + materialName + "': " +
                (matches ? "MATCH" : "NO MATCH"));
        return matches;
    }

    /**
     * Matches a string against a wildcard pattern.
     * Supports *NAME, NAME*, *NAME*, NAME*NAME, and *.
     * <p>
     * Special case: {@code *_SUFFIX} also matches the bare {@code SUFFIX}
     * (e.g. {@code *_SHULKER_BOX} matches undyed {@code SHULKER_BOX}).
     */
    public static boolean matchWildcard(String pattern, String text) {
        if (pattern.equals("*")) return true;
        if (!pattern.contains(String.valueOf(WILDCARD))) return pattern.equals(text);

        if (matchGlob(pattern, text)) {
            return true;
        }

        // *_FOO should also match undyed/base FOO (no color prefix)
        if (pattern.startsWith("*_") && pattern.length() > 2) {
            String bare = pattern.substring(2); // FOO from *_FOO
            if (text.equals(bare)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchGlob(String pattern, String text) {
        String[] parts = pattern.split("\\*", -1);
        int pos = 0;

        if (!parts[0].isEmpty()) {
            if (!text.startsWith(parts[0])) return false;
            pos = parts[0].length();
        }

        for (int i = 1; i < parts.length - 1; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            int idx = text.indexOf(part, pos);
            if (idx == -1) return false;
            pos = idx + part.length();
        }

        String last = parts[parts.length - 1];
        return last.isEmpty() || text.substring(pos).endsWith(last);
    }

    private static void logDebug(String msg) {
        if (Stackable.instance != null &&
                Stackable.instance.getConfig().getBoolean("debug", false)) {
            Stackable.instance.getLogger().log(Level.INFO, "[DEBUG] " + msg);
        }
    }
}
