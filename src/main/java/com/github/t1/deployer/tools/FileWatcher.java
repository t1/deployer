package com.github.t1.deployer.tools;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.Arrays.*;
import static java.util.concurrent.TimeUnit.*;

@Slf4j
public class FileWatcher extends Thread {
    private static final int POLL_TIMEOUT = 100;
    private static final WatchEvent.Kind[] EVENT_KINDS = { ENTRY_MODIFY, ENTRY_CREATE };
    private static final WatchEvent.Modifier[] EVENT_MODIFIERS = resolveEventModifiers();

    private static WatchEvent.Modifier[] resolveEventModifiers() {
        return new WatchEvent.Modifier[] {};
    }

    private final Path filePath;
    private final Runnable runnable;

    private final AtomicBoolean running = new AtomicBoolean(true);

    public FileWatcher(Path filePath, Runnable runnable) {
        super("FileWatcher:" + filePath);
        this.filePath = filePath;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        log.info("start watching {}", filePath);
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path path = filePath.getParent();
            path.register(watcher, EVENT_KINDS, EVENT_MODIFIERS);
            while (running.get()) {
                WatchKey key = watcher.poll(POLL_TIMEOUT, MILLISECONDS);
                if (key != null) {
                    @SuppressWarnings("unchecked")
                    List<WatchEvent<Path>> events = (List<WatchEvent<Path>>) (List) key.pollEvents();
                    log.debug("got watch key with {} events", events.size());
                    for (WatchEvent<Path> event : events)
                        handle(event);
                    key.reset();
                    log.debug("watch for next change");
                }
                Thread.yield();
            }
        } catch (IOException | InterruptedException e) {
            log.error("stop watching {} due to {} {}", filePath, e.getClass().getSimpleName(), e.getMessage());
            throw new RuntimeException("while watching " + filePath, e);
        }
        log.info("stop watching {}", filePath);
    }

    private void handle(WatchEvent<Path> event) {
        WatchEvent.Kind<?> kind = event.kind();

        if (kind == StandardWatchEventKinds.OVERFLOW) {
            log.debug("yield {} for {}", kind, event.context());
            Thread.yield();
        } else if (asList(EVENT_KINDS).contains(kind) && event.context().equals(filePath.getFileName())) {
            log.info("handle {} for {}", kind, event.context());
            runnable.run();
        } else {
            log.debug("skip {} for {}", kind, event.context());
        }
    }

    public void shutdown() { running.set(false); }
}
