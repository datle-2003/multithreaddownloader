package org.example.constants;

public class AppConstants {
    private AppConstants() {}

    public static final int DEFAULT_THREAD_COUNT = 4;
    public static final String DEFAULT_OUTPUT_FOLDER = "./";


    public static final int CONNECT_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 20000;

    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36";

    public static final int BUFFER_SIZE = 8192;

    public static final String FLAG_THREADS = "-n";
    public static final String FLAG_OUTPUT = "-o";
    public static final String FLAG_HELP = "-h";
}