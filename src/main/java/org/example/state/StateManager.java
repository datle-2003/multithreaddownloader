package org.example.state;

import java.io.*;

public class StateManager {
    private final File metaFile;
    private final File tmpFile;

    public StateManager(String destinationPath) {
        this.metaFile = new File(destinationPath + ".meta");
        this.tmpFile = new File(destinationPath + ".meta.tmp");
    }

    public synchronized void save(DownloadState state) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tmpFile))) {
            out.writeObject(state);
            out.flush();
        } catch (IOException e) {
            System.err.println("Warning: Could not save state to tmp file: " + e.getMessage());
            return;
        }

        if (metaFile.exists()) {
            if (!metaFile.delete()) {
                System.err.println("Warning: Could not delete old meta file.");
            }
        }

        if (!tmpFile.renameTo(metaFile)) {
            System.err.println("Warning: Could not rename tmp file to meta file.");
        }
    }

    public DownloadState load() {
        if (!metaFile.exists()) return null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(metaFile))) {
            return (DownloadState) in.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    public void delete() {
        if (metaFile.exists()) metaFile.delete();
        if (tmpFile.exists()) tmpFile.delete();
    }
}