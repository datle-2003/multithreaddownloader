package org.example.state;

import java.io.Serializable;


// state class aim to save the progress
public class DownloadState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private final String destinationPath;
    private final int threadCount;
    private final long contentLength;

    private final long[] downloadedBytesPerThread;

    public DownloadState(String url, String destinationPath, int threadCount, long contentLength) {
        this.url = url;
        this.destinationPath = destinationPath;
        this.threadCount = threadCount;
        this.contentLength = contentLength;
        this.downloadedBytesPerThread = new long[threadCount];
    }

    public void updateProgress(int workerId, long bytes) {
        downloadedBytesPerThread[workerId - 1] = bytes;
    }

    public long getProgress(int workerId) {
        return downloadedBytesPerThread[workerId - 1];
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public long getContentLength() {
        return contentLength;
    }
}