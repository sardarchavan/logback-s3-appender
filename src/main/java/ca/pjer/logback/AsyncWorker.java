package ca.pjer.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class AsyncWorker extends Worker implements Runnable {

    private final int maxBatchLogEvents;
    private final AtomicBoolean running;
    private Thread thread;
    private final List<String> events;

    AsyncWorker(AwsLogsAppender awsLogsAppender) {
        super(awsLogsAppender);
        maxBatchLogEvents = awsLogsAppender.getMaxBatchLogEvents();
        running = new AtomicBoolean(false);
        events = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public synchronized void start() {
        super.start();
        if (running.compareAndSet(false, true)) {
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.setName(getAwsLogsAppender().getName() + " Async Worker");
            thread.start();
        }
    }

    @Override
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            synchronized (running) {
                running.notifyAll();
            }
            if (thread != null) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    thread.interrupt();
                }
                thread = null;
            }
            events.clear();
        }
        super.stop();
    }

    @Override
    public void append(ILoggingEvent logEvent) {
        events.add(layoutEvent(logEvent));
        // trigger a flush if events is full
        if (events.size() >= maxBatchLogEvents) {
            synchronized (running) {
                running.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        while (running.get()) {
            flush(false);
            try {
                synchronized (running) {
                    if (running.get()) {
                        running.wait(getAwsLogsAppender().getMaxFlushTimeMillis());
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
            flush(true);
        }
        flush(true);
    }

    private void flush(boolean all) {
        try {
            if (events.size() <= 0) {
                getAwsLogsAppender().addInfo(" No events to log");
            } else if (all || events.size() >= maxBatchLogEvents) {
                getAwsLogsAppender().getAwsLogsStub().uploadToS3(events);
            }
        } catch (Exception e) {
            getAwsLogsAppender().addError("Unable to flush events to AWS", e);
        }
        events.clear();
    }
}
