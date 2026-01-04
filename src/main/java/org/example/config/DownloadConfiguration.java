package org.example.config;


import org.example.constants.AppConstants;


public class DownloadConfiguration {
    private String url;
    private String outputFilePath;
    private int threadCount;

    public DownloadConfiguration() {
        this.threadCount = AppConstants.DEFAULT_THREAD_COUNT;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        if (threadCount <= 0) {
            this.threadCount = AppConstants.DEFAULT_THREAD_COUNT;
        } else {
            this.threadCount = threadCount;
        }
    }

    public static class Builder {
        private final DownloadConfiguration config;

        public Builder() {
            this.config = new DownloadConfiguration();
        }

        public Builder setUrl(String url) {
            config.setUrl(url);
            return this;
        }

        public Builder setOutput(String path) {
            config.setOutputFilePath(path);
            return this;
        }

        public Builder setThreads(int n) {
            // Tận dụng logic validate đã viết trong setter của Config
            config.setThreadCount(n);
            return this;
        }

        public DownloadConfiguration build() {
            if (config.getUrl() == null || config.getUrl().isEmpty()) {
                throw new IllegalStateException("URL is required to build configuration");
            }
            return config;
        }
    }
}