package org.example.core;

import org.example.constants.AppConstants;
import org.example.state.DownloadState; // Import State
import org.example.utils.HttpUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadWorker implements Runnable {

    private final String fileUrl;
    private final String destinationPath;
    private final long startByte;
    private final long endByte;
    private final int workerId;
    private final CountDownLatch latch;
    private final AtomicLong totalDownloaded;
    private final DownloadState state;

    public DownloadWorker(String fileUrl, String destinationPath, long startByte, long endByte, int workerId, CountDownLatch latch, AtomicLong totalDownloaded, DownloadState state) {
        this.fileUrl = fileUrl;
        this.destinationPath = destinationPath;
        this.startByte = startByte;
        this.endByte = endByte;
        this.workerId = workerId;
        this.latch = latch;
        this.totalDownloaded = totalDownloaded;
        this.state = state;
    }

    @Override
    public void run() {
        int attempt = 0;
        boolean success = false;

        try {
            while (attempt < AppConstants.MAX_RETRIES && !success) { // try until max retries
                attempt++;

                long downloadedSoFar = state.getProgress(workerId); // load progress from state
                long currentStartByte = startByte + downloadedSoFar;

                if (currentStartByte > endByte) { // if the last one already finished -> break;
                    success = true;
                    break;
                }

                HttpURLConnection connection = null;

                try {
                    connection = HttpUtils.getConnection(fileUrl);

                    String range = "bytes=" + currentStartByte + "-" + endByte;
                    connection.setRequestProperty("Range", range);  // set range header

                    int responseCode = connection.getResponseCode();

                    // Not 206 or 200 -> error
                    if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                        System.err.println("Worker " + workerId + ": Server error " + responseCode + ". Aborting.");
                        return;
                    }

                    // got 200 OK but startByte > 0 -> invalid
                    if (startByte > 0 && responseCode == HttpURLConnection.HTTP_OK) {
                        System.err.println("Worker " + workerId + ": Server sent full file instead of partial. Aborting.");
                        return;
                    }

                    long localTotal = downloadedSoFar; // store local total for this worker

                    try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
                         RandomAccessFile raf = new RandomAccessFile(destinationPath, "rw")) {

                        raf.seek(currentStartByte);

                        byte[] buffer = new byte[AppConstants.BUFFER_SIZE];
                        int bytesRead;

                        long totalReadThisSession = 0L;

                        long remainingBytes = endByte - currentStartByte + 1;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            int toWrite = bytesRead;
                            if (totalReadThisSession + bytesRead > remainingBytes) {
                                toWrite = (int) (remainingBytes - totalReadThisSession); // to keep this worker don't exceed its range
                            }

                            raf.write(buffer, 0, toWrite); // write to file

                            totalDownloaded.addAndGet(toWrite); // update to draw progress bar

                            localTotal += toWrite;
                            state.updateProgress(workerId, localTotal); // update state progress to save progress

                            totalReadThisSession += toWrite;
                            if (totalReadThisSession >= remainingBytes) break;
                        }
                        success = true;
                    }

                } catch (FileNotFoundException e) {
                    System.err.println("Worker " + workerId + ": File path error. Aborting.");
                    return;
                } catch (IOException e) {
                    if (e instanceof UnknownHostException) { // invalid domain/host -> abort
                        System.err.println("Worker " + workerId + ": Invalid Domain/Host. Aborting.");
                        return;
                    }
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