package org.example.core;

import org.example.config.DownloadConfiguration;
import org.example.state.DownloadState;
import org.example.state.StateManager;
import org.example.utils.FileUtils;
import org.example.utils.HttpUtils;

import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


public class DownloadManager {
    private final DownloadConfiguration config;
    private int nThreads;

    public DownloadManager(DownloadConfiguration config) {
        this.config = config;
        this.nThreads = config.getThreadCount();
    }

    public void download() {
        ExecutorService executorService = null;
        CountDownLatch latch = null;

        AtomicLong totalDownloaded = new AtomicLong(0);
        StateManager stateManager = new StateManager(config.getOutputFilePath());

        Thread progressThread = null;
        Thread saveThread = null;

        try {
            // get remote file size
            long remoteContentLength = HttpUtils.getContentLength(config.getUrl());
            if (remoteContentLength <= 0) {
                System.err.println("Cannot retrieve remote file size or file is empty.");
                return;
            }

           // check support range (206 Partial Content)
            boolean isSupportRange = HttpUtils.isSupportRange(config.getUrl());
            if (!isSupportRange) {
                this.nThreads = 1; // single thread
                System.out.println("Server does not support Range requests. Using single thread.");
            }

            // load state from file
            DownloadState state = stateManager.load();
            boolean isResuming = false;

            if (state != null) {
                if (state.getContentLength() != remoteContentLength) {
                    System.err.println("File size has changed since last download. Cannot resume.");
                    state = new DownloadState(config.getUrl(), config.getOutputFilePath(), this.nThreads, remoteContentLength);
                }
                else if (!isSupportRange) {
                    System.out.println("Server does not support Range requests. Cannot resume.");
                    state = new DownloadState(config.getUrl(), config.getOutputFilePath(), this.nThreads, remoteContentLength);
                }
                else {
                    System.out.println("Resuming download from previous state...");
                    isResuming = true;

                    // override thread count, if thread is changed -> conflict with saved state
                    if (state.getThreadCount() != this.nThreads) {
                        System.out.println("Using previous thread count from saved state: " + state.getThreadCount());
                        this.nThreads = state.getThreadCount();
                    }

                    // if the URL has changed, but still download the same file
                    if (!state.getUrl().equals(config.getUrl())) {
                        System.out.println("URL has changed. Updating saved state.");
                        state.setUrl(config.getUrl());
                        stateManager.save(state);
                    }

                    // update totalDownloaded to handle progress bar
                    long recoveredBytes = 0;
                    for (int i = 1; i <= this.nThreads; i++) {
                        recoveredBytes += state.getProgress(i);
                    }
                    totalDownloaded.set(recoveredBytes);
                }
            } else {
                // no resume file found, create new state
                state = new DownloadState(config.getUrl(), config.getOutputFilePath(), this.nThreads, remoteContentLength);
            }

            // create a new file if flag is not resuming
            // if resuming, the file should already exist
            if (!isResuming) {
                try (RandomAccessFile raf = new RandomAccessFile(config.getOutputFilePath(), "rw")) {
                    raf.setLength(remoteContentLength);
                }
            } else {
                // double check file size
                try (RandomAccessFile raf = new RandomAccessFile(config.getOutputFilePath(), "rw")) {
                    if (raf.length() != remoteContentLength) {
                        System.err.println("Existing file size does not match remote file size. Cannot resume.");
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Error accessing existing file. Cannot resume.");
                    return;
                }
            }

            // create thread pool and latch counter
            executorService = Executors.newFixedThreadPool(this.nThreads);
            latch = new CountDownLatch(this.nThreads);
            long partSize = remoteContentLength / this.nThreads;

            // display progress bar
            progressThread = new Thread(() -> {
                long lastDownloaded = 0;
                long lastTime = System.currentTimeMillis();
                String speedStr = "0 B/s";

                try {
                    while (totalDownloaded.get() < remoteContentLength && !Thread.currentThread().isInterrupted()) {
                        Thread.sleep(1000);
                        long currentDownloaded = totalDownloaded.get();
                        long currentTime = System.currentTimeMillis();

                        long timeDiff = currentTime - lastTime;
                        if (timeDiff > 0) {
                            long bytesDiff = currentDownloaded - lastDownloaded;
                            long speedBytesPerSec = (bytesDiff * 1000) / timeDiff;
                            speedStr = FileUtils.byteCountToDisplaySize(speedBytesPerSec) + "/s";
                        }

                        printProgressBar(currentDownloaded, remoteContentLength, speedStr);

                        lastDownloaded = currentDownloaded;
                        lastTime = currentTime;
                    }

                    if (totalDownloaded.get() >= remoteContentLength) {
                        printProgressBar(totalDownloaded.get(), remoteContentLength, "Done");
                        System.out.println();
                    }
                } catch (InterruptedException e) {
                }
            });

            progressThread.start();

            // auto save thread
            DownloadState finalStateForSave = state; // create a final reference for lambda
            saveThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) { // not received interrupt signal (Ctrl+C or program end)
                    try {
                        Thread.sleep(1000);
                        stateManager.save(finalStateForSave); // save for each 1 second
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                stateManager.save(finalStateForSave);
            });
            saveThread.start();

            final ExecutorService finalExecutor = executorService;
            final Thread finalUIThread = progressThread;
            final Thread finalSaveThread = saveThread;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {

                if (finalExecutor != null) finalExecutor.shutdownNow();
                if (finalUIThread != null) finalUIThread.interrupt();


                if (finalSaveThread != null) {
                    finalSaveThread.interrupt();
                    try {
                        finalSaveThread.join(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }));

            for (int i = 0; i < this.nThreads; i++) {
                long startByte = i * partSize;
                long endByte = (i == this.nThreads - 1) ? remoteContentLength - 1 : (startByte + partSize - 1);

                DownloadWorker worker = new DownloadWorker(
                        config.getUrl(),
                        config.getOutputFilePath(),
                        startByte,
                        endByte,
                        i + 1,
                        latch,
                        totalDownloaded,
                        state
                );
                executorService.submit(worker);
            }

            latch.await();

            if (saveThread != null) {
                saveThread.interrupt();
                try {
                    saveThread.join(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (progressThread != null) progressThread.interrupt();

            if (totalDownloaded.get() >= remoteContentLength) {
                stateManager.delete();
                System.out.println("\nDownload completed: " + config.getOutputFilePath());
            } else {
                System.out.println("\nDownload stopped incomplete. Resume file saved.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }
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