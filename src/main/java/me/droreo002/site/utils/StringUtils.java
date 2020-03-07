package me.droreo002.site.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {

    /**
     * Get the string between boundaries
     * specified with regex
     *
     * @param source The string source
     * @param regex The string regex
     * @return Result string
     */
    public static String getStringBetween(String source, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
