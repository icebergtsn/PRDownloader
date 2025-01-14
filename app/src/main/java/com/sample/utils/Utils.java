package com.sample.utils;

import java.util.Locale;

/**
 * Created by amitshekhar on 13/11/17.
 */

public final class Utils {

    private Utils() {
        // no instance
    }

    public static String getProgressDisplayLine(long currentBytes, long totalBytes) {
        return getBytesToMBString(currentBytes) + "/" + getBytesToMBString(totalBytes);
    }

    private static String getBytesToMBString(long bytes) {
        return String.format(Locale.ENGLISH, "%.2fMb", bytes / (1024.00 * 1024.00));
    }

}
