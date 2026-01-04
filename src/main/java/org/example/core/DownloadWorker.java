package org.example.core;

import org.example.constants.AppConstants;
import org.example.utils.HttpUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

public class DownloadWorker implements Runnable {

    private final String fileUrl;
    private final String destinationPath;
    private final long startByte;
    private final long endByte;
    private final int workerId;
    private final CountDownLatch latch;

    public DownloadWorker(String fileUrl, String destinationPath, long startByte, long endByte, int workerId, CountDownLatch latch) {
        this.fileUrl = fileUrl;
        this.destinationPath = destinationPath;
        this.startByte = startByte;
        this.endByte = endByte;
        this.workerId = workerId;
        this.latch = latch;
    }

    @Override
    public void run() {
        int attempt = 0;
        boolean success = false;

        try { // Try lớn này để đảm bảo finally latch.countDown() chỉ chạy 1 lần
            while (attempt < AppConstants.MAX_RETRIES && !success) {
                attempt++;
                HttpURLConnection connection = null;

                try {
                    connection = HttpUtils.getConnection(fileUrl);

                    // Setup Timeout cho mỗi lần thử lại
                    connection.setConnectTimeout(AppConstants.CONNECT_TIMEOUT);
                    connection.setReadTimeout(AppConstants.READ_TIMEOUT);

                    String range = "bytes=" + startByte + "-" + endByte;
                    connection.setRequestProperty("Range", range);

                    int responseCode = connection.getResponseCode();

                    if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                        System.err.println("Worker " + workerId + ": Server error " + responseCode + ". Aborting.");
                        return;
                    }

                    if (startByte > 0 && responseCode == HttpURLConnection.HTTP_OK) {
                        System.err.println("Worker " + workerId + ": Server sent full file instead of partial. Aborting.");
                        return;
                    }

                    try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
                         RandomAccessFile raf = new RandomAccessFile(destinationPath, "rw")) {

                        raf.seek(startByte);

                        byte[] buffer = new byte[AppConstants.BUFFER_SIZE];
                        int bytesRead;
                        long totalRead = 0L;
                        long expected = endByte - startByte + 1;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            int toWrite = bytesRead;
                            if (totalRead + bytesRead > expected) {
                                toWrite = (int) (expected - totalRead);
                            }
                            raf.write(buffer, 0, toWrite);
                            totalRead += toWrite;
                            if (totalRead >= expected) break;
                        }
                        success = true;
//                        System.out.println("Worker " + workerId + ": Downloaded success.");
                    }

                } catch (FileNotFoundException e) {
                    System.err.println("Worker " + workerId + ": File path error. Aborting.");
                    return;
                } catch (IOException e) {
                    System.err.println("Worker " + workerId + ": Error (Attempt " + attempt + "/" + AppConstants.MAX_RETRIES + "): " + e.getMessage());

                    if (attempt < AppConstants.MAX_RETRIES) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {}
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            if (!success) {
                System.err.println("Worker " + workerId + ": FAILED after " + AppConstants.MAX_RETRIES + " attempts.");
            }
        } finally {
            latch.countDown();
        }
    }
}