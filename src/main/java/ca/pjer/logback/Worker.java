package ca.pjer.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

abstract class Worker {

    private AwsLogsAppender awsLogsAppender;

    Worker(AwsLogsAppender awsLogsAppender) {
        this.awsLogsAppender = awsLogsAppender;
    }

    AwsLogsAppender getAwsLogsAppender() {
        return awsLogsAppender;
    }

    public synchronized void start() {
    }

    public synchronized void stop() {
    }

    String layoutEvent(ILoggingEvent event) {
        return awsLogsAppender.getLayout().doLayout(event);
    }

    OutputStream createCompressedStreamAsNecessary(
        OutputStream outputStream, boolean compressEnabled) throws IOException {
        Objects.requireNonNull(outputStream);
        if (compressEnabled) {
            return new GZIPOutputStream(outputStream);
        } else {
            return outputStream;
        }
    }

    public abstract void append(ILoggingEvent event);

}
