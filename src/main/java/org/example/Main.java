package org.example;

import org.example.config.DownloadConfiguration;
import org.example.core.DownloadManager;
import org.example.utils.ArgumentParser;

public class Main {
    public static void main(String[] args) {
        try {
            DownloadConfiguration config = ArgumentParser.parseArguments(args);

            System.out.println("Target: " + config.getUrl());
            System.out.println("Output: " + config.getOutputFilePath());
            System.out.println("Threads: " + config.getThreadCount());

            DownloadManager manager = new DownloadManager(config);

            manager.download();

        } catch (ArgumentParser.HelpRequestException e) {
            ArgumentParser.printHelp();
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error: " + e.getMessage());
            System.out.println();
            ArgumentParser.printHelp();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}