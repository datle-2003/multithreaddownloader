package org.example.utils;


import java.util.Locale;

public final class FileUtils {
    private FileUtils() {}

    public static String byteCountToDisplaySize(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }

        final long KB = 1024L;
        final long MB = KB * 1024L;
        final long GB = MB * 1024L;
        final long TB = GB * 1024L;

        if (bytes < KB) {
            return bytes + " B";
        } else if (bytes < MB) {
            double value = (double) bytes / KB;
            return String.format(Locale.US, "%.1f KB", value);
        } else if (bytes < GB) {
            double value = (double) bytes / MB;
            return String.format(Locale.US, "%.1f MB", value);
        } else if (bytes < TB) {
            double value = (double) bytes / GB;
            return String.format(Locale.US, "%.1f GB", value);
        } else {
            double value = (double) bytes / TB;
            return String.format(Locale.US, "%.1f TB", value);
        }
    }
}
