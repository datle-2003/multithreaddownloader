package org.example.utils;

import org.example.config.DownloadConfiguration;
import org.example.constants.AppConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentParserTest {

    @Test
    void parseUrlOnlyUsesDefaults() {
        String url = "https://example.com/path/file.txt";
        DownloadConfiguration cfg = ArgumentParser.parseArguments(new String[]{url});

        assertEquals(url, cfg.getUrl());
        assertEquals("file.txt", cfg.getOutputFilePath());
        assertEquals(AppConstants.DEFAULT_THREAD_COUNT, cfg.getThreadCount());
    }

    @Test
    void parseWithThreadsAndOutput() {
        String url = "https://example.com/a.bin";
        DownloadConfiguration cfg = ArgumentParser.parseArguments(new String[]{url, "-n", "4", "-o", "/tmp/out.bin"});

        assertEquals(url, cfg.getUrl());
        assertEquals(4, cfg.getThreadCount());
        assertEquals("/tmp/out.bin", cfg.getOutputFilePath());
    }

    @Test
    void unknownOptionThrows() {
        String url = "https://example.com/a.bin";
        assertThrows(IllegalArgumentException.class, () -> ArgumentParser.parseArguments(new String[]{"-x", url}));
    }

    @Test
    void helpFlagThrowsHelpRequestException() {
        assertThrows(ArgumentParser.HelpRequestException.class, () -> ArgumentParser.parseArguments(new String[]{"-h"}));
    }
}
