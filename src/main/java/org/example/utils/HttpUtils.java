package org.example.utils;

import org.example.constants.AppConstants;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class HttpUtils {
    public static HttpURLConnection getConnection(String fileUrl) throws IOException {
        URI uri = URI.create(fileUrl);
        URL url = uri.toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty("User-Agent", AppConstants.USER_AGENT);
        connection.setConnectTimeout(AppConstants.CONNECT_TIMEOUT);
        connection.setReadTimeout(AppConstants.READ_TIMEOUT);

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

    public static boolean isSupportRange(String fileUrl) throws IOException {
        HttpURLConnection connection = getConnection(fileUrl);

        // try to request a single byte
        connection.setRequestProperty("Range", "bytes=0-0");

        int responseCode = connection.getResponseCode();
        connection.disconnect();

        return responseCode == HttpURLConnection.HTTP_PARTIAL; // 206
    }
}
