package org.example.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

class HttpUtilsTest {

    // URL ổn định để test (Google logo hoặc file text nhỏ)
    private static final String TEST_URL = "https://www.google.com/robots.txt";
    private static final String FAKE_URL = "https://www.google.com/nonexistentfile.txt";

    @Test
    @DisplayName("Connection successful")
    void testGetConnection_Success() throws IOException {
        HttpURLConnection connection = HttpUtils.getConnection(TEST_URL);

        Assertions.assertNotNull(connection, "Connection must not be null");

        int responseCode = connection.getResponseCode();
        Assertions.assertEquals(200, responseCode, "Response code must be 200 OK");
    }

    @Test
    @DisplayName("Get content length successfully")
    void testGetContentLength() throws IOException {
        long size = HttpUtils.getContentLength(TEST_URL);

        Assertions.assertTrue(size > 0, "Content length must be greater than 0");
    }

    @Test
    @DisplayName("Handle 404 Not Found")
    void testGetConnection_NotFound() throws IOException {
        HttpURLConnection connection = HttpUtils.getConnection(FAKE_URL);

        int responseCode = connection.getResponseCode();
        Assertions.assertEquals(404, responseCode, "For nonexistent file, response code must be 404 Not Found");
    }
}