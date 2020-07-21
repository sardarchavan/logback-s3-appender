package ca.pjer.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.layout.EchoLayout;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.after;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AsyncWorkerTest {

    private static List<String> anyLogEvents() {
        return anyList();
    }

    private static final AtomicLong timestamp = new AtomicLong(System.currentTimeMillis());

    private static ILoggingEvent dummyEvent() {
        LoggerContext loggerContext = new LoggerContext();
        LoggingEvent event = new LoggingEvent(AsyncWorkerTest.class.getName(),
            loggerContext.getLogger(AsyncWorkerTest.class.getName()), Level.WARN,
            "Dummy " + UUID.randomUUID().toString(), null, null);
        event.setTimeStamp(timestamp.getAndIncrement());
        return event;
    }

    private static AsyncWorker asyncWorker(AWSLogsStub mockedAwsLogsStub, int maxBatchLogEvents,
        long maxFlushTimeMillis) {
        AwsLogsAppender awsLogsAppender = new AwsLogsAppender();
        awsLogsAppender.setLayout(new EchoLayout<ILoggingEvent>());
        awsLogsAppender.setBucketName("FakeBucket");
        awsLogsAppender.setBucketPath("FakePath");
        awsLogsAppender.setMaxBatchLogEvents(maxBatchLogEvents);
        awsLogsAppender.setMaxFlushTimeMillis(maxFlushTimeMillis);
        awsLogsAppender.setAwsLogsStub(mockedAwsLogsStub);
        AsyncWorker asyncWorker = new AsyncWorker(awsLogsAppender);
        awsLogsAppender.setWorker(asyncWorker);
        return asyncWorker;
    }

    @Test
    public void testShouldNotLogWhenStopped() {
        AWSLogsStub mockedAwsLogsStub = mock(AWSLogsStub.class);
        AsyncWorker asyncWorker = asyncWorker(mockedAwsLogsStub, 1, 1);
        asyncWorker.start();
        asyncWorker.stop();
        asyncWorker.append(dummyEvent());
        verify(mockedAwsLogsStub, never()).uploadToS3(anyLogEvents());
    }

    @Test
    public void testShouldLogWhenStarted() {
        AWSLogsStub mockedAwsLogsStub = mock(AWSLogsStub.class);
        AsyncWorker asyncWorker = asyncWorker(mockedAwsLogsStub, 1, 1);
        asyncWorker.start();
        asyncWorker.append(dummyEvent());
        asyncWorker.stop();
        verify(mockedAwsLogsStub, atLeastOnce()).uploadToS3(anyLogEvents());
    }

    @Test
    public void testShouldLogAfterMaxBatchSize() {
        AWSLogsStub mockedAwsLogsStub = mock(AWSLogsStub.class);
        AsyncWorker asyncWorker = asyncWorker(mockedAwsLogsStub, 5, Long.MAX_VALUE);
        asyncWorker.start();
        asyncWorker.append(dummyEvent());
        asyncWorker.append(dummyEvent());
        asyncWorker.append(dummyEvent());
        asyncWorker.append(dummyEvent());
        verify(mockedAwsLogsStub, never()).uploadToS3(anyLogEvents());
        asyncWorker.append(dummyEvent());
        asyncWorker.stop();
        verify(mockedAwsLogsStub, atLeastOnce()).uploadToS3(anyLogEvents());
    }

    @Test
    public void testShouldNotLogEmptyAfterMaxFlushTimeMillis() {
        AWSLogsStub mockedAwsLogsStub = mock(AWSLogsStub.class);
        AsyncWorker asyncWorker = asyncWorker(mockedAwsLogsStub, 1, 1);
        asyncWorker.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            return;
        }
        verify(mockedAwsLogsStub, never()).uploadToS3(anyLogEvents());
    }

    @Test
    public void testShouldLogAfterMaxFlushTimeMillis() {
        AWSLogsStub mockedAwsLogsStub = mock(AWSLogsStub.class);
        AsyncWorker asyncWorker = asyncWorker(mockedAwsLogsStub, 5, 1000);
        asyncWorker.start();
        asyncWorker.append(dummyEvent());
        verify(mockedAwsLogsStub, after(1500)).uploadToS3(anyLogEvents());
    }
}
