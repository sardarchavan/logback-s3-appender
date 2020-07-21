package ca.pjer.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class AwsLogsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private Layout<ILoggingEvent> layout;

    private String bucketName;
    private String bucketPath;
    private String s3Region;
    private int maxBatchLogEvents = 50;
    private long maxFlushTimeMillis = 0;
    private AWSLogsStub awsLogsStub;
    private Worker worker;

    @SuppressWarnings({"unused", "WeakerAccess"})
    public Layout<ILoggingEvent> getLayout() {
        return layout;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public String getBucketName() {
        return bucketName;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public String getBucketPath() {
        return bucketPath;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setBucketPath(String bucketPath) {
        this.bucketPath = bucketPath;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public String getS3Region() {
        return s3Region;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public int getMaxBatchLogEvents() {
        return maxBatchLogEvents;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setMaxBatchLogEvents(int maxBatchLogEvents) {
        this.maxBatchLogEvents = maxBatchLogEvents;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})

    public long getMaxFlushTimeMillis() {
        return maxFlushTimeMillis;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setMaxFlushTimeMillis(long maxFlushTimeMillis) {
        this.maxFlushTimeMillis = maxFlushTimeMillis;
    }

    AWSLogsStub getAwsLogsStub() {
        return awsLogsStub;
    }

    void setAwsLogsStub(AWSLogsStub awsLogsStub) {
        this.awsLogsStub = awsLogsStub;
    }

    void setWorker(Worker worker) {
        this.worker = worker;
    }

    @Override
    public synchronized void start() {
        if (!isStarted()) {
            if (this.awsLogsStub == null) {
                AWSLogsStub awsLogsStub = new AWSLogsStub(bucketName, bucketPath, s3Region);
                this.awsLogsStub = awsLogsStub;
                awsLogsStub.start();
            }
            if (this.worker == null) {
                Worker worker = new AsyncWorker(this);
                this.worker = worker;
                worker.start();
            }
            super.start();
        }
    }

    @Override
    public synchronized void stop() {
        if (isStarted()) {
            super.stop();
            if (worker != null) {
                worker.stop();
                worker = null;
            }
            if (awsLogsStub != null) {
                awsLogsStub.stop();
                awsLogsStub = null;
            }
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (worker != null) {
            worker.append(event);
        }
    }
}
