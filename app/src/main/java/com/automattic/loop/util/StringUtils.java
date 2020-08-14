package com.automattic.loop.util;

public class StringUtils {
    /*
     * returns empty string if passed string is null, otherwise returns passed string
     */
    public static String notNullStr(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

    /*
     * simple wrapper for Integer.valueOf(string) so caller doesn't need to catch NumberFormatException
     */
    public static int stringToInt(String s) {
        return stringToInt(s, 0);
    }

    public static int stringToInt(String s, int defaultValue) {
        if (s == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
