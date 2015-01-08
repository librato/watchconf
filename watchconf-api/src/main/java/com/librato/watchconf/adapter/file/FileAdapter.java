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

public class FileAdapter<T> extends AbstractConfigAdapter<T> {

    private static final Logger log = Logger.getLogger(FileAdapter.class);
    private final String path;
    private final File file;
    private final Executor fileWatchExecutor = Executors.newSingleThreadExecutor();

    public FileAdapter(String path, Converter<T> converter) throws IOException, InterruptedException {
        super(converter);
        Preconditions.checkArgument(path != null && !path.isEmpty(), "path cannot be null or blank");
        Preconditions.checkNotNull(converter, "converter cannot be null");
        this.path = stripSlash(path);
        this.file = new File(path);

        getAndSet();

        WatchService watcher = FileSystems.getDefault().newWatchService();
        Path dir = Paths.get(path.substring(0, path.lastIndexOf("/")));
        WatchQueueReader fileWatcher = new WatchQueueReader(watcher, dir.toString(), this);
        dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        fileWatchExecutor.execute(fileWatcher);
    }

    private String stripSlash(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public void getAndSet() {
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fileInputStream.read(data);
            config.set(Optional.of(converter.toDomain(data, clazz)));
        } catch (Exception ex) {
            log.error("unable to parse config", ex);
        } finally {
            if (fileInputStream != null)
                try {
                    fileInputStream.close();
                } catch (IOException ex) {
                    log.error("error closing FileInputStream", ex);
                }
        }
    }

    private static class WatchQueueReader implements Runnable {

        private final String fileName;
        private final FileAdapter adapter;
        /**
         * the watchService that is passed in from above
         */
        private WatchService watcher;

        public WatchQueueReader(WatchService watcher, String fileName, FileAdapter adapter) {
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
                        // get event type
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fileName = ev.context();
                        if (this.fileName.equals(fileName)) {
                            adapter.getAndSet();
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

