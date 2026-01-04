package org.example.state;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class StateManagerTest {

    @Test
    void testSaveLoadDeleteState() throws Exception {
        File tmp = File.createTempFile("state-manager-test", ".bin");
        String basePath = tmp.getAbsolutePath();
        // remove the temp file itself; StateManager uses path + ".meta"
        tmp.delete();

        StateManager manager = new StateManager(basePath);

        DownloadState s1 = new DownloadState("u", basePath, 2, 1234L);
        s1.updateProgress(1, 100L);
        s1.updateProgress(2, 200L);

        manager.save(s1);

        DownloadState loaded = manager.load();
        assertNotNull(loaded);
        assertEquals(2, loaded.getThreadCount());
        assertEquals(1234L, loaded.getContentLength());
        assertEquals(100L, loaded.getProgress(1));
        assertEquals(200L, loaded.getProgress(2));

        manager.delete();

        // meta and tmp should be removed
        File meta = new File(basePath + ".meta");
        File tmpFile = new File(basePath + ".meta.tmp");
        assertFalse(meta.exists());
        assertFalse(tmpFile.exists());
    }
}