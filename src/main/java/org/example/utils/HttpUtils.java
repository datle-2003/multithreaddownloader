package org.example.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class HttpUtils {
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36";

    private static final int CONNECTION_TIMEOUT = 5000; // wait max 5 seconds to establish a connection
    private static final int READ_TIMEOUT = 10000; //  wait max 10 seconds to read data


    public static HttpURLConnection getConnection(String fileUrl) throws IOException {
        URI uri = URI.create(fileUrl);
        URL url = uri.toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);

        connection.setInstanceFollowRedirects(true);

        return connection;
    }

    public static long getContentLength(String fileUrl) throws IOException {
        HttpURLConnection connection = getConnection(fileUrl);
        connection.setRequestMethod("HEAD"); // fetch header only

        long length = connection.getContentLengthLong();
        connection.disconnect();

        return length;
    }
}
