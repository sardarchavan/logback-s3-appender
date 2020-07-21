package ca.pjer.logback;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.apache.http.entity.ContentType;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

class AWSLogsStub {

    private final String bucketName;
    private final String bucketPath;
    private final String s3Region;

    private final Lazy<AmazonS3> lazyAwsS3 = new Lazy<>();

    AWSLogsStub(String bucketName, String bucketPath, String s3Region) {
        this.bucketName = bucketName;
        this.bucketPath = bucketPath;
        this.s3Region = s3Region;
    }

    private AmazonS3 awsS3() {
        return lazyAwsS3.getOrCompute(() -> {
            AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(s3Region)
                .build();
            return s3;
        });
    }

    synchronized void start() {

    }

    synchronized void stop() {
        try {
            awsS3().shutdown();
        } catch (Exception e) {
            // ignore
        }
    }

    synchronized void uploadToS3(List<String> logEvents) {
        File file = writeLogsToTempFile(logEvents);
        if (file.length() > 0) {
            String key = getS3ObjectKey();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType(ContentType.DEFAULT_BINARY.getMimeType());
            PutObjectRequest por = new PutObjectRequest(bucketName, key, file);
            por.setMetadata(metadata);
            awsS3().putObject(por);
        }
        file.delete();
    }

    private String getS3ObjectKey() {
        String fileExtension = ".log.gz";
        String hostName;
        try {
            java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
            hostName = addr.getHostName();
        } catch (UnknownHostException e) {
            hostName = "";
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        return String.format("%s/%s_%s%s", bucketPath, hostName, df.format(new Date()), fileExtension);
    }

    private File writeLogsToTempFile(List<String> logEvents) {
        File tempFile = null;
        Writer writer = null;
        try {
            tempFile = File.createTempFile("tmp", null);
            OutputStream os = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));
            writer = new OutputStreamWriter(os);
            // FileUtils.writeLines(tempFile, events);
            for (String event : logEvents) {
                writer.write(event);
            }
        } catch (IOException e) {
            new AwsS3Appender().addError(e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    new AwsS3Appender().addError(e.getMessage());
                }
            }
        }
        return tempFile;
    }
}
