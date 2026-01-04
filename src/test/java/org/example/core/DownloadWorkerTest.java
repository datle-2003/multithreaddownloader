package org.example.core;

import org.example.state.DownloadState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class DownloadWorkerTest {

    private static final String TEST_URL = "https://www.google.com/robots.txt";
    private File tempFile;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = File.createTempFile("test_worker", ".txt");
    }

    @AfterEach
    void tearDown() {
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    @DisplayName("Worker download 50 bytes and save to file correctly")
    void testWorkerDownloadsPartialContent() throws InterruptedException, IOException {
        long startByte = 0;
        long endByte = 49;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong totalDownloaded = new AtomicLong(0);

        DownloadState state = new DownloadState(TEST_URL, tempFile.getAbsolutePath(), 1, 1000);

        DownloadWorker worker = new DownloadWorker(
                TEST_URL,
                tempFile.getAbsolutePath(),
                startByte,
                endByte,
                1,
                latch,
                totalDownloaded,
                state
        );

        Thread thread = new Thread(worker);
        thread.start();

        boolean completed = latch.await(10, TimeUnit.SECONDS);

        assertTrue(completed, "Worker must complete within timeout");
        assertTrue(tempFile.length() > 0, "File must not be empty after download");

        assertEquals(50, tempFile.length(), "File size must be 50 bytes");

        System.out.println("Test Passed! File content preview: " + Files.readString(tempFile.toPath()));
    }

    @Test
    @DisplayName("Worker handles bad URL gracefully")
    void testWorkerWithBadUrl() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        DownloadWorker worker = getDownloadWorker(latch);

        new Thread(worker).start();

        boolean completed = latch.await(10, TimeUnit.SECONDS);

        assertTrue(completed, "Worker must complete (even with failure) within timeout");
    }

    private DownloadWorker getDownloadWorker(CountDownLatch latch) {
        AtomicLong totalDownloaded = new AtomicLong(0);
        String badUrl = "https://invalid-url-blabla-xyz-123.com/file.txt";

        // Dummy state cho test lỗi
        DownloadState state = new DownloadState(badUrl, tempFile.getAbsolutePath(), 1, 100);

        return new DownloadWorker(
                badUrl,
                tempFile.getAbsolutePath(),
                0, 100, 1, latch,
                totalDownloaded,
                state
        );
    }
}