package org.example.core;

import org.example.config.DownloadConfiguration;
import org.example.utils.FileUtils;
import org.example.utils.HttpUtils;

import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadManager {
    private final DownloadConfiguration config;

    public DownloadManager(DownloadConfiguration config) {
        this.config = config;
    }

    public void download() {
        ExecutorService executorService = null;
        CountDownLatch latch = null;
        int threadCount = config.getThreadCount();

        try {
            boolean isSupportRange = HttpUtils.isSupportRange(config.getUrl());

            if (!isSupportRange) {
                threadCount = 1;
                System.out.println("Server does not support range requests. Downloading with a single thread.");
            }

            long contentLength = HttpUtils.getContentLength(config.getUrl());
            if (contentLength <= 0) {
                System.err.println("Could not retrieve content length. Aborting download.");
                return;
            }

            // create an empty file at destination path
            try (RandomAccessFile raf = new RandomAccessFile(config.getOutputFilePath(), "rw")) {
                raf.setLength(contentLength);
            }

            executorService = Executors.newFixedThreadPool(threadCount);
            latch = new CountDownLatch(threadCount);



            long partSize = contentLength / threadCount;

            AtomicLong totalDownloaded = new AtomicLong(0);

            Thread progressThread = new Thread(() -> {
                long lastDownloaded = 0;
                long lastTime = System.currentTimeMillis();
                String speedStr = "0 B/s";

                try {
                    while (totalDownloaded.get() < contentLength) {
                        Thread.sleep(1000);
                        long currentDownloaded = totalDownloaded.get();
                        long currentTime = System.currentTimeMillis();

                        long timeDiff = currentTime - lastTime;

                        if (timeDiff > 0) {
                            long bytesDiff = currentDownloaded - lastDownloaded;

                            long speedBytesPerSec = (bytesDiff * 1000) / timeDiff;
                            speedStr = FileUtils.byteCountToDisplaySize(speedBytesPerSec) + "/s";
                        } else {
                            speedStr = "0 B/s";
                        }

                        printProgressBar(currentDownloaded, contentLength, speedStr);

                        lastDownloaded = currentDownloaded;
                        lastTime = currentTime;

                    }
                    printProgressBar(totalDownloaded.get(), contentLength, speedStr);
                    System.out.println();
                } catch (InterruptedException e) {
                }
            });
            progressThread.start();

            final ExecutorService finalExecutor = executorService;
            final Thread finalUIThread = progressThread;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n\nStopping download and cleaning up...");

                finalExecutor.shutdownNow();

                if (finalUIThread != null) {
                    finalUIThread.interrupt();
                }

                System.out.println("Download interrupted by user.");
            }));

            for (int i = 0; i < threadCount; i++) {
                long startByte = i * partSize;
                long endByte = (i == threadCount - 1) ? contentLength - 1 : (startByte + partSize - 1);

                DownloadWorker worker = new DownloadWorker(
                        config.getUrl(),
                        config.getOutputFilePath(),
                        startByte,
                        endByte,
                        i + 1,
                        latch,
                        totalDownloaded
                );

                executorService.submit(worker);
            }

            latch.await();

            System.out.println("\nDownload completed: " + config.getOutputFilePath());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }

    // Thêm tham số String currentSpeed
    private void printProgressBar(long current, long total, String currentSpeed) {
        if (total <= 0) return;

        int width = 50;
        int progress = (int) ((current * 100) / total);
        int charsToPrint = (int) ((current * width) / total);

        StringBuilder bar = new StringBuilder();
        bar.append("\r[");

        for (int i = 0; i < width; i++) {
            if (i < charsToPrint) bar.append("=");
            else if (i == charsToPrint) bar.append(">");
            else bar.append(" ");
        }
        bar.append("] ");

        String currentSize = FileUtils.byteCountToDisplaySize(current);
        String totalSize = FileUtils.byteCountToDisplaySize(total);

        String sizeInfo = String.format("%d%% (%s / %s) - %s    ",
                progress, currentSize, totalSize, currentSpeed);

        bar.append(sizeInfo);
        System.out.print(bar.toString());
    }
}