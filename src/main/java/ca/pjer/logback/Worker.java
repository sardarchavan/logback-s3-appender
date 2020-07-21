package ca.pjer.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

abstract class Worker {

    private AwsS3Appender awsS3Appender;

    Worker(AwsS3Appender awsS3Appender) {
        this.awsS3Appender = awsS3Appender;
    }

    AwsS3Appender getAwsS3Appender() {
        return awsS3Appender;
    }

    public synchronized void start() {
    }

    public synchronized void stop() {
    }

    String layoutEvent(ILoggingEvent event) {
        return awsS3Appender.getLayout().doLayout(event);
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
