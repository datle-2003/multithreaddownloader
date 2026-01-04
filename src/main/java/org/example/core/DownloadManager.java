package org.example.core;

import org.example.config.DownloadConfiguration;
import org.example.utils.HttpUtils;

import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

            // calculate the size of each part
            // the last part will take the remaining bytes
            long partSize = contentLength / threadCount;

            for (int i = 0; i < threadCount; i++) {
                long startByte = i * partSize;
                long endByte = (i == threadCount - 1) ? contentLength - 1 : (startByte + partSize - 1);

                DownloadWorker worker = new DownloadWorker(
                        config.getUrl(),
                        config.getOutputFilePath(),
                        startByte,
                        endByte,
                        i + 1,
                        latch
                );

                executorService.submit(worker);
            }

            latch.await();

            System.out.println("Download completed: " + config.getOutputFilePath());
            System.out.println("Total size: " + contentLength + " bytes");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }
}