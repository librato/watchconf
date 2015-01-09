package com.librato.watchconf.adapter.file;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.librato.watchconf.adapter.AbstractConfigAdapter;
import com.librato.watchconf.converter.Converter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class DynamicConfigFileAdapter<T> extends AbstractConfigAdapter<T, byte[]> {

    private static final Logger log = Logger.getLogger(DynamicConfigFileAdapter.class);
    private final File file;
    private final Executor fileWatchExecutor = Executors.newSingleThreadExecutor();

    public DynamicConfigFileAdapter(String path, Converter<T, byte[]> converter, ChangeListener<T> changeListener) throws IOException, InterruptedException {
        super(converter, Optional.fromNullable(changeListener));
        Preconditions.checkArgument(path != null && !path.isEmpty(), "path cannot be null or blank");
        Preconditions.checkArgument(converter != null, "converter cannot be null");
        this.file = new File(stripSlash(path));

        getAndSet(readFile());

        WatchService watcher = FileSystems.getDefault().newWatchService();
        Path dir = Paths.get(path.substring(0, path.lastIndexOf("/")));
        WatchQueueReader fileWatcher = new WatchQueueReader(watcher, path.substring(path.lastIndexOf("/") + 1), this);
        dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        fileWatchExecutor.execute(fileWatcher);
    }

    public DynamicConfigFileAdapter(String path, Converter<T, byte[]> converter) throws IOException, InterruptedException {
        this(path, converter, null);
    }

    private String stripSlash(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public byte[] readFile() {
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fileInputStream.read(data);
            return data;
        } catch (Exception ex) {
            log.error("error reading file", ex);
        } finally {
            if (fileInputStream != null)
                try {
                    fileInputStream.close();
                } catch (IOException ex) {
                    log.error("error closing FileInputStream", ex);
                }
        }

        return null;
    }

    private static class WatchQueueReader implements Runnable {

        private final String fileName;
        private final DynamicConfigFileAdapter adapter;
        /**
         * the watchService that is passed in from above
         */
        private WatchService watcher;

        public WatchQueueReader(WatchService watcher, String fileName, DynamicConfigFileAdapter adapter) {
            this.watcher = watcher;
            this.fileName = fileName;
            this.adapter = adapter;
        }

        /**
         * In order to implement a file watcher, we loop forever
         * ensuring requesting to take the next item from the file
         * watchers queue.
         */
        @Override
        public void run() {
            while (true) {
                WatchKey key = null;
                // wait for a key to be available
                try {
                    key = watcher.poll(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                if (key != null) {

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fileName = ev.context();
                        if (this.fileName.equals(fileName.toString())) {
                            adapter.getAndSet(adapter.readFile());
                            adapter.notifyListeners();
                        }
                    }

                    // IMPORTANT: The key must be reset after processed
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            }
        }
    }
}

