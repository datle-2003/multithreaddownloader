package org.example.core;

import org.example.config.DownloadConfiguration;
import org.example.state.StateManager;
import org.example.utils.HttpUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class DownloadManagerTest {

    @Test
    void download_submitsWorkersAndRemovesState() throws Exception {
        File tmp = File.createTempFile("dmtest", ".bin");
        String basePath = tmp.getAbsolutePath();
        tmp.delete();

        int nThreads = 3;
        long remoteLength = 300L;
        String url = "https://example.com/file.bin";

        DownloadConfiguration config = new DownloadConfiguration.Builder()
                .setUrl(url)
                .setOutput(basePath)
                .setThreads(nThreads)
                .build();

        try (MockedStatic<HttpUtils> httpMock = Mockito.mockStatic(HttpUtils.class);
             MockedConstruction<StateManager> sm = Mockito.mockConstruction(StateManager.class, (mock, ctx) -> {
                 Mockito.when(mock.load()).thenReturn(null);
             });
             MockedConstruction<DownloadWorker> wc = Mockito.mockConstruction(DownloadWorker.class, (mock, ctx) -> {
                 CountDownLatch latch = (CountDownLatch) ctx.arguments().get(5);
                 AtomicLong total = (AtomicLong) ctx.arguments().get(6);
                 long start = (Long) ctx.arguments().get(2);
                 long end = (Long) ctx.arguments().get(3);
                 Mockito.doAnswer(inv -> {
                     total.addAndGet(end - start + 1);
                     latch.countDown();
                     return null;
                 }).when(mock).run();
             })) {

            httpMock.when(() -> HttpUtils.getContentLength(url)).thenReturn(remoteLength);
            httpMock.when(() -> HttpUtils.isSupportRange(url)).thenReturn(true);

            DownloadManager dm = new DownloadManager(config);
            dm.download();

            // ensure number of workers created matches thread count
            assertEquals(nThreads, wc.constructed().size());

            // verify state manager delete was called after completion
            StateManager stateMock = sm.constructed().get(0);
            Mockito.verify(stateMock).delete();
        }
    }
}
