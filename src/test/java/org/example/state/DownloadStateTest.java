package org.example.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DownloadStateTest {

    @Test
    void testConstructorAndProgressMethods() {
        DownloadState state = new DownloadState("http://example.com/file", "/tmp/out.bin", 3, 10000L);

        assertEquals(3, state.getThreadCount());
        assertEquals(10000L, state.getContentLength());
        assertEquals("http://example.com/file", state.getUrl());

        state.updateProgress(2, 512L);
        assertEquals(512L, state.getProgress(2));

        state.setUrl("http://example.com/updated");
        assertEquals("http://example.com/updated", state.getUrl());
    }
}