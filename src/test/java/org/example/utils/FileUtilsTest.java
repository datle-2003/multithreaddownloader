package org.example.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @Test
    void testByteCountToDisplaySize_variousSizes() {
        assertEquals("0 B", FileUtils.byteCountToDisplaySize(0));
        assertEquals("500 B", FileUtils.byteCountToDisplaySize(500));
        assertEquals("1.5 KB", FileUtils.byteCountToDisplaySize(1536)); // 1.5 * 1024
        assertEquals("2.0 MB", FileUtils.byteCountToDisplaySize(2L * 1024 * 1024));
        assertEquals("3.0 GB", FileUtils.byteCountToDisplaySize(3L * 1024 * 1024 * 1024));
        assertEquals("5.0 TB", FileUtils.byteCountToDisplaySize(5L * 1024 * 1024 * 1024 * 1024));
    }
}