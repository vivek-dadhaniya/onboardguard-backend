package com.onboardguard.screening.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * All name-comparison logic lives here so both strategies share it.
 *
 * Jaro-Winkler is used for fuzzy matching because:
 * - It gives extra weight to prefix matches ("Sharma" vs "Shrma") which is
 *   common in name typos.
 * - It handles short strings better than Levenshtein normalized distance.
 * - It runs in O(n*m) — fast enough for screening 100+ watchlist entries.
 */
@Component
public class NameMatchingUtil {

    /**
     * Normalize a name for comparison:
     * - Smartly separates unspaced initials (e.g., "RS" -> "R S")
     * - lowercase & trim
     * - converts dots to spaces (e.g., "R.S." -> "R S")
     * - collapses multiple spaces to one
     * - removes all punctuation
     */
    public String normalize(String name) {
        if (name == null) return "";

        // Handle unspaced capital initials (e.g., "RS Sharma" -> "R S Sharma")
        // This Regex says: Find an Uppercase letter that is immediately followed by another Uppercase letter
        String preProcessed = name.replaceAll("([A-Z])(?=[A-Z])", "$1 ");

        return preProcessed.toLowerCase()
                .trim()
                // Convert dots to spaces so isExactMatch works for "A.B." -> "A. B. "
                .replace(".", " ")
                .replaceAll("\\s+", " ")
                // remove everything except letters, digits, spaces, and dots
                .replaceAll("[^a-z0-9 .]", "");
    }

    public boolean isExactMatch(String a, String b) {
        if (a == null || b == null) return false;
        return normalize(a).equals(normalize(b));
    }

    /**
     * Jaro-Winkler similarity between two strings.
     * Returns 0.0 (no similarity) to 1.0 (identical).
     */
    public double jaroWinkler(String a, String b) {
        if (a == null || b == null) return 0.0;
        String s1 = normalize(a);
        String s2 = normalize(b);
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        double jaro = jaro(s1, s2);

        // Winkler prefix bonus — up to 4 matching prefix characters
        int prefixLen = 0;
        int limit = Math.min(4, Math.min(s1.length(), s2.length()));
        for (int i = 0; i < limit; i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefixLen++;
            else break;
        }

        return jaro + (prefixLen * 0.1 * (1.0 - jaro));
    }

    /**
     * Whether two strings are a fuzzy match given a threshold (0.0–1.0).
     * Default threshold from config is 0.80.
     */
    public boolean isFuzzyMatch(String a, String b, double threshold) {
        return jaroWinkler(a, b) >= threshold;
    }

    /**
     * Initials-expansion match.
     *
     * "R.S. Sharma" vs "Rohit S. Sharma"
     *  → tokens: ["r.", "s.", "sharma"]  vs ["rohit", "s.", "sharma"]
     *  → "r." is an initial, check first char of "rohit" → 'r' == 'r' ✓
     *  → "s." is an initial, compare with "s." directly → match ✓
     *  → "sharma" vs "sharma" → exact ✓
     *  → Result: MATCH
     *
     * Returns true only if ALL tokens can be matched (strict full-name match).
     */
    public boolean isInitialsExpansionMatch(String candidateName, String watchlistName) {
        if (candidateName == null || watchlistName == null) return false;

        List<String> cTokens = tokenize(normalize(candidateName));
        List<String> wTokens = tokenize(normalize(watchlistName));

        // Must have the same number of tokens to be a valid match
        if (cTokens.size() != wTokens.size()) return false;

        for (int i = 0; i < cTokens.size(); i++) {
            String ct = cTokens.get(i);
            String wt = wTokens.get(i);

            if (isInitial(ct)) {
                // candidate token is an initial - check against first letter of watchlist token
                char initial = ct.charAt(0);
                if (wt.isEmpty() || wt.charAt(0) != initial) return false;
            } else if (isInitial(wt)) {
                // watchlist token is an initial - check against first letter of candidate token
                char initial = wt.charAt(0);
                if (ct.isEmpty() || ct.charAt(0) != initial) return false;
            } else {
                // Both are full tokens - require exact match here
                // (fuzzy is handled separately at the token level via isFuzzyMatch)
                if (!ct.equals(wt)) return false;
            }
        }
        return true;
    }

    /**
     * Full advanced name comparison combining initials expansion AND fuzzy matching.
     *
     * First tries initials expansion (handles "R.S. Sharma" vs "Rohit S. Sharma").
     * Then falls back to whole-name Jaro-Winkler (handles "Rohit Shrma" vs "Rohit Sharma").
     *
     * Returns a NameMatchResult containing whether it matched and the similarity score.
     */
    public NameMatchResult advancedNameMatch(String candidateName, String watchlistName, double threshold) {
        if (candidateName == null || watchlistName == null) {
            return NameMatchResult.noMatch();
        }

        String normCandidate = normalize(candidateName);
        String normWatchlist = normalize(watchlistName);

        // 1. Exact match
        if (normCandidate.equals(normWatchlist)) {
            return NameMatchResult.exactMatch();
        }

        // 2. Initials expansion
        if (isInitialsExpansionMatch(normCandidate, normWatchlist)) {
            // Treat initials expansion as high-confidence — score it as fuzzy with 0.90
            return NameMatchResult.fuzzy(0.90);
        }

        // 3. Whole-name Jaro-Winkler
        double sim = jaroWinkler(normCandidate, normWatchlist);
        if (sim >= threshold) {
            return NameMatchResult.fuzzy(sim);
        }

        return NameMatchResult.noMatch();
    }

    /**
     * Split a normalized name into tokens by space.
     */
    private List<String> tokenize(String name) {
        List<String> tokens = new ArrayList<>();
        for (String t : name.split(" ")) {
            if (!t.isBlank()) tokens.add(t.trim());
        }
        return tokens;
    }

    /**
     * A token is considered an initial if it is a single letter optionally
     * followed by a dot. Examples: "r", "r.", "s."
     */
    private boolean isInitial(String token) {
        return token.matches("^[a-z]\\.?$");
    }

    /**
     * Core Jaro similarity (without the Winkler prefix adjustment).
     */
    private double jaro(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int matchWindow = Math.max(len1, len2) / 2 - 1;
        if (matchWindow < 0) matchWindow = 0;

        boolean[] s1Matched = new boolean[len1];
        boolean[] s2Matched = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchWindow);
            int end   = Math.min(i + matchWindow + 1, len2);
            for (int j = start; j < end; j++) {
                if (!s2Matched[j] && s1.charAt(i) == s2.charAt(j)) {
                    s1Matched[i] = true;
                    s2Matched[j] = true;
                    matches++;
                    break;
                }
            }
        }

        if (matches == 0) return 0.0;

        int transpositions = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (s1Matched[i]) {
                while (!s2Matched[k]) k++;
                if (s1.charAt(i) != s2.charAt(k)) transpositions++;
                k++;
            }
        }

        return (((double) matches / len1)
                + ((double) matches / len2)
                + ((double) (matches - transpositions / 2) / matches)) / 3.0;
    }

    // Inner result type

    /**
     * Immutable result of an advanced name comparison.
     */
    public record NameMatchResult(
            boolean matched,
            boolean exact,
            double similarity
    ) {

        public static NameMatchResult noMatch() {
            return new NameMatchResult(false, false, 0.0);
        }

        public static NameMatchResult exactMatch() {
            return new NameMatchResult(true, true, 1.0);
        }

        public static NameMatchResult fuzzy(double similarity) {
            return new NameMatchResult(true, false, similarity);
        }
    }
}