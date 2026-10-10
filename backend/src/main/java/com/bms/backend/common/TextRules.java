package com.bms.backend.common;

/**
 * Ported from Version 1. One shared rule for one-line text fields: no control or "invisible" characters.
 * A line break inside a name breaks tables, CSV files and logs, and the invisible right-to-left
 * override characters can make a name look like something else on screen.
 */
public final class TextRules {

    private TextRules() {
    }

    /** Null is fine here; the "required" rule is checked separately. */
    public static void requireSingleLine(String value, String label) {
        if (value != null && containsBadCharacter(value)) {
            throw new ValidationException("The " + label
                    + " contains characters that are not allowed (such as line breaks, tabs or hidden characters).");
        }
    }

    private static boolean containsBadCharacter(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c)                       // includes NUL, newline, tab
                    || (c >= '​' && c <= '‏')         // zero-width and direction marks
                    || (c >= '‪' && c <= '‮')         // direction embedding / override
                    || (c >= '⁠' && c <= '⁤')         // word joiner and invisible operators
                    || (c >= '⁦' && c <= '⁩')         // direction isolates
                    || c == '﻿') {                         // zero-width no-break space
                return true;
            }
        }
        return false;
    }
}
