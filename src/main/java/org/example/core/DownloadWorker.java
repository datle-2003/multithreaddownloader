package org.example.core;

import org.example.utils.HttpUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class DownloadWorker implements Runnable{

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
        HttpURLConnection connection = null;

        try {
            connection = HttpUtils.getConnection(fileUrl);
            String range = "bytes=" + startByte + "-" + endByte;
            connection.setRequestProperty("Range", range);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Worker " + workerId + ": Server does not support partial content. Response code: " + responseCode);
                return;
            }

            if (startByte > 0 && responseCode == HttpURLConnection.HTTP_OK) {
                System.err.println("Worker " + workerId + " Panic! Asked for byte " + startByte + " but got full file (200). Stopping.");
                return;
            }

            try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
                 RandomAccessFile raf = new RandomAccessFile(destinationPath, "rw")) {

                raf.seek(startByte); // move the file pointer to start byte

                byte[] buffer = new byte[4096];
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

                System.out.println("Worker " + workerId + ": Downloaded " + totalRead + " bytes from " + startByte + " to " + endByte);

            } catch (FileNotFoundException e) {
                System.err.println("Worker " + workerId + ": Destination file not found.");
                return;
            }
        } catch (IOException e) {
            System.err.println("Worker " + workerId + ": Failed to establish connection or IO error: " + e.getMessage());
            return;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            latch.countDown();
        }
    }
}
