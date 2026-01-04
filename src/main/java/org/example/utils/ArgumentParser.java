package org.example.utils;

import org.example.config.DownloadConfiguration;
import org.example.constants.AppConstants;

import java.net.URI;
import java.nio.file.Paths;

import static java.lang.Math.max;

public class ArgumentParser {

    public static DownloadConfiguration parseArguments(String[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("No arguments provided. Use -h for help.");
        }

        DownloadConfiguration config = new DownloadConfiguration();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case AppConstants.FLAG_HELP -> {
                    throw new HelpRequestException();
                }

                case AppConstants.FLAG_THREADS -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for threads after " + AppConstants.FLAG_THREADS);
                    }
                    try {
                        int nThreads = Integer.parseInt(args[++i]);
                        config.setThreadCount(nThreads);
                        if (nThreads <= 0) {
                            System.err.println("Warning: Invalid thread count (" + nThreads + "). Using default.");
                        }

                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid number of threads: " + args[i]);
                    }
                }

                case AppConstants.FLAG_OUTPUT -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for output path after " + AppConstants.FLAG_OUTPUT);
                    }
                    config.setOutputFilePath(args[++i]);
                }

                default -> {
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    }
                    if (config.getUrl() != null) {
                        throw new IllegalArgumentException("Multiple URLs provided or invalid argument: " + arg);
                    }
                    config.setUrl(arg);
                }
            }
        }

        if (config.getUrl() == null || config.getUrl().isEmpty()) {
            throw new IllegalArgumentException("URL is required!");
        }

        if (config.getOutputFilePath() == null) {
            config.setOutputFilePath(getFileNameFromUrl(config.getUrl()));
        }

        return config;
    }
    private static String getFileNameFromUrl(String urlString) {
        try {
            URI uri = URI.create(urlString);
            String path = uri.getPath();
            if (path == null || path.isEmpty() || path.equals("/")) return "downloaded_file.dat";

            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            return "downloaded_file.dat";
        }
    }

    public static void printHelp() {
        int cores = Runtime.getRuntime().availableProcessors();
        int recommendedThreads = max(1, cores - 1);

        System.out.println("Java Multi-threaded Downloader");
        System.out.println("Usage: java -jar downloader.jar [URL] [OPTIONS]");
        System.out.println("Options:");
        System.out.println("  -n <number>   Number of threads (Default: " + AppConstants.DEFAULT_THREAD_COUNT + ")");
        System.out.println("  -o <path>     Output file path (Default: derived from URL)");
        System.out.println("  -h            Show help");
        System.out.println("Example:");
        System.out.println("  java -jar downloader.jar https://example.com/file.iso -n 8 -o my_movie.iso");
        System.out.println("Recommended number of threads based on your CPU: " + recommendedThreads);

    }

    public static class HelpRequestException extends RuntimeException {}
}
