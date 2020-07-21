[![Build Status](https://travis-ci.org/pierredavidbelanger/logback-awslogs-appender.svg?branch=master)](https://travis-ci.org/pierredavidbelanger/logback-awslogs-appender)

# Logback AWS S3 appender
Thank you for your help:
- [pierredavidbelanger](https://github.com/pierredavidbelanger/logback-awslogs-appende)

## Quick start

### `pom.xml`:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.21</version>
        </dependency>
        <dependency>
            <groupId>ca.pjer</groupId>
            <artifactId>logback-s3-appender</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</project>
```

### `logback.xml`

The simplest config that actually (synchronously) send logs to CloudWatch (see [More configurations section](#more-configurations) for a real life example):

```xml
<configuration>

    <appender name="AWS_S3" class="ca.pjer.logback.AwsS3Appender"/>

    <root>
        <appender-ref ref="AWS_S3"/>
    </root>

</configuration>
```

With every possible defaults:
- The Layout will default to [EchoLayout](http://logback.qos.ch/apidocs/ch/qos/logback/core/layout/EchoLayout.html).
- The AWS Region will default to the AWS SDK default region (`us-east-1`) or the current instance region.
- The `maxFlushTimeMillis` will default to `0`, so appender is in synchronous mode.

`AwsS3Appender` will search for AWS Credentials using the [DefaultAWSCredentialsProviderChain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html).


### Code

As usual with [SLF4J](http://www.slf4j.org/):

```java
// get a logger
org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MyClass.class);

// log
logger.info("HelloWorld");
```

## More configurations

It may be worth quoting this from _AWS_, beacause this is why we need to have unique `logStreamName`:

> When you have two processes attempting to perform the PutLogEvents API call to the same log stream, there is a chance that one will pass and one will fail because of the sequence token provided for that same log stream. Because of the sequencing of these events maintained in the log stream, you cannot have concurrently running processes pushing to the same log-stream.

A real life `logback.xml` would probably look like this (when all options are specified):

```xml
<configuration packagingData="true">

    <!-- Register the shutdown hook to allow logback to cleanly stop appenders -->
    <!-- this is strongly recommend when using AwsS3Appender in async mode, -->
    <!-- to allow the queue to flush on exit -->
    <shutdownHook class="ch.qos.logback.core.hook.DelayingShutdownHook"/>

    <!-- Timestamp used into the Log Stream Name -->
    <timestamp key="timestamp" datePattern="yyyyMMddHHmmssSSS"/>

    <!-- The actual AwsS3Appender (asynchronous mode because of maxFlushTimeMillis > 0) -->
    <appender name="ASYNC_AWS_S3" class="ca.pjer.logback.AwsS3Appender">
    
        <!-- Send only WARN and above -->
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>WARN</level>
        </filter>
        
        <!-- Nice layout pattern -->
        <layout>
            <pattern>%d{yyyyMMdd'T'HHmmss} %thread %level %logger{15} %msg%n</pattern>
        </layout>
        
        <!-- S3 Bucket Name -->
        <bucketName>my-bucket</bucketName>

        <!-- S3 Bucket path -->
        <bucketPath>myapp</bucketPath>
    
        <!-- S3 region-->
        <s3Region>ap-southeast-2</s3Region>
        
        <!-- Maximum number of events in each batch (50 is the default) -->
        <!-- will flush when the event queue has 50 elements, even if still in quiet time (see maxFlushTimeMillis) -->
        <maxBatchLogEvents>50</maxBatchLogEvents>
        
        <!-- Maximum quiet time in millisecond (0 is the default) -->
        <!-- will flush when met, even if the batch size is not met (see maxBatchLogEvents) -->
        <maxFlushTimeMillis>30000</maxFlushTimeMillis>
    </appender>

    <!-- A console output -->
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyyMMdd'T'HHmmss} %thread %level %logger{15} %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Root with a threshold to INFO and above -->
    <root level="INFO">
        <!-- Append to the console -->
        <appender-ref ref="STDOUT"/>
        <!-- Append also to the (async) AwsS3Appender -->
        <appender-ref ref="ASYNC_AWS_LOGS"/>
    </root>

</configuration>
```

See [The logback manual - Chapter 3: Logback configuration](http://logback.qos.ch/manual/configuration.html) for more config options.
