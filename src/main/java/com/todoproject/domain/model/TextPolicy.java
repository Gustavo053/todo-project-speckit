package com.todoproject.domain.model;

import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.ViolationCode;
import java.text.Normalizer;
import java.util.Set;

/**
 * The allowlist character policy for all stored text (research R5).
 *
 * <p>This is deny-by-default, as constitution principle III requires: a code point is accepted only
 * when its Unicode category appears in {@link #PERMITTED_CATEGORIES}. That is what blocks the
 * invisible classes — zero-width characters and bidirectional overrides are both category Cf — as
 * well as control characters, surrogates, private-use and unassigned code points.
 *
 * <p>Accented Portuguese letters are category Lu/Ll and emoji are So, so both are accepted.
 *
 * <p>Plain Java only: no {@code jakarta.validation}, because the domain must not carry a framework
 * dependency (constitution principle II).
 */
public final class TextPolicy {

    private static final Set<Integer> PERMITTED_CATEGORIES = Set.of(
            // L* letters
            (int) Character.UPPERCASE_LETTER,
            (int) Character.LOWERCASE_LETTER,
            (int) Character.TITLECASE_LETTER,
            (int) Character.MODIFIER_LETTER,
            (int) Character.OTHER_LETTER,
            // M* marks (accents that are stored separately after normalization)
            (int) Character.NON_SPACING_MARK,
            (int) Character.ENCLOSING_MARK,
            (int) Character.COMBINING_SPACING_MARK,
            // N* numbers
            (int) Character.DECIMAL_DIGIT_NUMBER,
            (int) Character.LETTER_NUMBER,
            (int) Character.OTHER_NUMBER,
            // Zs space separator (the ordinary space; Zl and Zp are deliberately absent)
            (int) Character.SPACE_SEPARATOR,
            // P* punctuation
            (int) Character.DASH_PUNCTUATION,
            (int) Character.START_PUNCTUATION,
            (int) Character.END_PUNCTUATION,
            (int) Character.CONNECTOR_PUNCTUATION,
            (int) Character.OTHER_PUNCTUATION,
            (int) Character.INITIAL_QUOTE_PUNCTUATION,
            (int) Character.FINAL_QUOTE_PUNCTUATION,
            // S* symbols
            (int) Character.MATH_SYMBOL,
            (int) Character.CURRENCY_SYMBOL,
            (int) Character.MODIFIER_SYMBOL,
            (int) Character.OTHER_SYMBOL);

    private static final char LINE_FEED = '\n';

    private TextPolicy() {}

    /**
     * Normalizes a single-line value: NFC, trimmed, with internal whitespace runs collapsed to one
     * space. A value that is whitespace only becomes empty, which is what makes the
     * whitespace-only rejection in FR-007 concrete.
     */
    public static String normalizeSingleLine(String raw) {
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFC);
        return normalized.replaceAll("\\s+", " ").trim();
    }

    /**
     * Normalizes a multi-line value: NFC, line endings unified to {@code \n}, trailing whitespace
     * removed from each line, and the whole value trimmed. Line structure is preserved.
     */
    public static String normalizeMultiLine(String raw) {
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFC)
                .replace("\r\n", "\n")
                .replace('\r', LINE_FEED);
        StringBuilder builder = new StringBuilder(normalized.length());
        String[] lines = normalized.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append(LINE_FEED);
            }
            builder.append(lines[i].stripTrailing());
        }
        return builder.toString().strip();
    }

    /**
     * Rejects any code point whose category is not permitted.
     *
     * @param allowLineFeed whether {@code \n} is acceptable; true for notes, false for titles
     */
    public static void requireAllowedCharacters(String text, boolean allowLineFeed, String field) {
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            if (!isAllowed(codePoint, allowLineFeed)) {
                throw new InvalidCardContentException(field, ViolationCode.FORBIDDEN_CHARACTER);
            }
            index += Character.charCount(codePoint);
        }
    }

    /** Length in code points, so a character outside the basic plane counts once. */
    public static int length(String text) {
        return text.codePointCount(0, text.length());
    }

    private static boolean isAllowed(int codePoint, boolean allowLineFeed) {
        if (allowLineFeed && codePoint == LINE_FEED) {
            return true;
        }
        return PERMITTED_CATEGORIES.contains(Character.getType(codePoint));
    }
}
