# Echopraxia Spring Boot Example

This is a sample Spring Boot application that shows structured logging using [Echopraxia](https://github.com/tersesystems/echopraxia).

## Running

Using `./gradlew bootRun` will start the application.

You can also run Spring Boot in debug mode:

```bash
./gradlew bootRun --args='--debug'
```

## Gradle

Adding Echopraxia to Spring Boot is straightforward.  We add the logstash implementation of Echopraxia to `build.gradle`:

```groovy
dependencies {
    implementation "com.tersesystems.echopraxia:logger:$version" // or use :simple:
    implementation "com.tersesystems.echopraxia:logstash:$version"
    implementation "com.tersesystems.echopraxia:scripting:$version"
    // for the system info filter
    implementation 'com.github.oshi:oshi-core:6.6.5'

    // typically you also want the latest version of logstash-logback-encoder as well..
    implementation 'net.logstash.logback:logstash-logback-encoder:8.0'
}
```

If you would rather use Log4J, you will need to exclude the `spring-boot-starter-logging` implementation, and then add `spring-boot-starter-log4j2` and `spring-boot-starter-web` explicitly:

```groovy
configurations {
    implementation.exclude module: 'spring-boot-starter-logging'
}

dependencies {
    implementation "com.tersesystems.echopraxia:log4j:$version"
    implementation "com.tersesystems.echopraxia:scripting:$version"

	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.boot:spring-boot-starter-log4j2'
	implementation 'org.apache.logging.log4j:log4j-layout-template-json:2.17.1'

	testImplementation('org.springframework.boot:spring-boot-starter-test')
}
```

## Changing Logging Levels

You can change the logging levels dynamically by going to [http://localhost:8080/actuator/loggers](http://localhost:8080/actuator/loggers), through [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/actuator.html#actuator.loggers).

You can trigger the greeting controller path by going to [http://localhost:8080/](http://localhost:8080/).

## GreetingController

The greeting controller is where the logger is created.

Here, we'll set up a custom field builder that can extract elements out of an HTTP request, then use it to ensure that contextual data is extracted when logging.

Here's the whole `GreetingController` code:

```java

@RestController
public class GreetingController {

  private static final String template = "Hello, %s!";
  private final AtomicLong counter = new AtomicLong();

  private final Logger<HttpRequestFieldBuilder> logger =
    LoggerFactory.getLogger(getClass(), HttpRequestFieldBuilder.instance)
      .withFields(
        fb ->
          // Any fields that you set in context you can set conditions on later,
          // i.e. on the URI path, content type, or extra headers.
          // These fields will be visible in the JSON file, not shown in console.
          fb.requestFields(httpServletRequest())
      );

  @NotNull
  private HttpServletRequest httpServletRequest() {
    return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
  }

  @GetMapping("/")
  public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
    // Log using a field builder to add a greeting_name field to JSON
    logger.info("Greetings {}", fb -> fb.string("greeting_name", name));

    // Clear MDC on every request...
    MDC.clear();

    // You can put MDC in the current thread, and threadContext/threadLocal methods will work
    MDC.put("contextKey", "contextValue");
    MDC.put("currentInstant", Instant.now().toString());

    // and have it available as conditions and fields
    Condition c = Condition.anyMatch(p -> Objects.equals(p.name(), "contextKey"));
    logger.withThreadContext().info(c, "Calling withThreadContext() adds MDC variables!");

    return new Greeting(counter.incrementAndGet(), String.format(template, name));
  }
}
```

## HttpRequestFieldBuilder

The custom field builder `HttpRequestFieldBuilder` is implemented as follows:

```java
public class HttpRequestFieldBuilder implements Field.Builder {
  public List<Field> requestFields(HttpServletRequest request) {
    Field urlField = string("request_uri", request.getRequestURI());
    Field methodField = string("request_method", request.getMethod());
    Field remoteAddressField = string("request_remote_addr", request.getRemoteAddr());
    return Arrays.asList(urlField, methodField, remoteAddressField);
  }
}
```

The default `string` fields are simple key value pairs that get returned as a list.

## System Info Filter

In addition to the explicitly defined fields added through loggers, there's another layer of filters that runs through the core logger factory.  For example, using [OSHI](https://github.com/oshi/oshi), you can add system information to every logger at once:

```java
public class SystemInfoFilter implements CoreLoggerFilter {

    private static final FieldBuilder fieldBuilder = FieldBuilder.instance();

    private final SystemInfo systemInfo;

    public SystemInfoFilter() {
        systemInfo = new SystemInfo();
    }

    @Override
    public CoreLogger apply(CoreLogger coreLogger) {
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        GlobalMemory mem = hardware.getMemory();
        CentralProcessor proc = hardware.getProcessor();
        double[] loadAverage = proc.getSystemLoadAverage(3);

        // Now you can add conditions based on these fields, and conditionally
        // enable logging based on your load and memory!
        return coreLogger.withFields(
                fb -> {
                    Field loadField =
                            fb.object(
                                    "load_average", //
                                    fb.number("1min", loadAverage[0]), //
                                    fb.number("5min", loadAverage[1]), //
                                    fb.number("15min", loadAverage[2]));
                    Field memField =
                            fb.object(
                                    "mem", //
                                    fb.number("available", mem.getAvailable()), //
                                    fb.number("total", mem.getTotal()));
                    Field sysinfoField = fb.object("sysinfo", loadField, memField);
                    return (sysinfoField);
                },
                fieldBuilder);
    }
}
```

## Spring Boot Logging

There are two configurations available, Logback and Log4J.

### Logback

The implementation is done through `logback-spring.xml`:

```xml
<configuration>

    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- logs to /tmp/spring.ndjson by default -->
    <!--    <property name="LOG_FILE" value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}/}spring.ndjson}"/>-->
    <property name="LOG_FILE" value="spring.ndjson"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <!-- show at most DEBUG on console -->
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>DEBUG</level>
        </filter>
        <encoder>
            <pattern>%date{H:mm:ss.SSS} %highlight(%-5level) [%thread]: %message%n%ex</pattern>
        </encoder>
    </appender>

    <!-- https://github.com/logfellow/logstash-logback-encoder -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <jsonGeneratorDecorator class="net.logstash.logback.decorate.PrettyPrintingJsonGeneratorDecorator"/>
            <providers>
                <timestamp>
                    <timeZone>UTC</timeZone>
                </timestamp>
                <version/>
                <message/>
                <loggerName/>
                <threadName/>
                <logLevel/>
                <logLevelValue/><!-- numeric value is useful for filtering >= -->
                <stackHash/>
                <!-- <mdc/> --> <!-- not showing mdc as we want to demo withContext() -->
                <logstashMarkers/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>

        <!-- use the default spring boot conventions here, but leverage a different encoder -->
        <file>${LOG_FILE}</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz}</fileNamePattern>
            <cleanHistoryOnStart>${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}</cleanHistoryOnStart>
            <maxFileSize>${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}</maxFileSize>
            <totalSizeCap>${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}</totalSizeCap>
            <maxHistory>${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- should be using the disruptor appender by default -->
    <appender name="ASYNCFILE" class="net.logstash.logback.appender.LoggingEventAsyncDisruptorAppender">
        <appender-ref ref="FILE"/>
    </appender>

    <logger name="com.example.restservice" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="ASYNCFILE" />
    </root>
</configuration>
```

The file appender logs to `/tmp/spring.log` and contains JSON like this:

```json
 {
  "@timestamp" : "2021-12-29T16:05:31.043-08:00",
  "@version" : "1",
  "message" : "Greetings World",
  "logger_name" : "com.example.restservice.GreetingController",
  "thread_name" : "http-nio-8080-exec-1",
  "level" : "INFO",
  "level_value" : 20000,
  "request_uri" : "/greeting",
  "request_remote_addr" : "127.0.0.1",
  "request_method" : "GET",
  "greeting_name" : "World"
}
```

Note the `request_uri`, `request_remote_addr`, and `request_method` fields, which come from context, and the `greeting_name` which comes from an explicit argument.

### Log4J

For Log4J, the implementation is in `log4j-spring.xml` with a packages pointing to `echopraxia.log4j.layout`:

```xml
<Configuration status="WARN" packages="echopraxia.log4j.layout">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT" follow="true">
            <JsonTemplateLayout
                    eventTemplateUri="classpath:LogstashJsonEventLayoutCustom.json"
                    locationInfoEnabled="true"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

and the `LogstashJsonEventLayoutCustom.json` contains an `echopraxiaFields` resolver:

```json
{
  "fields": {
    "$resolver": "echopraxiaFields"
  },
  "mdc": {
    "$resolver": "mdc"
  },
  "exception": {
    "exception_class": {
      "$resolver": "exception",
      "field": "className"
    },
    "exception_message": {
      "$resolver": "exception",
      "field": "message",
      "stringified": true
    },
    "stacktrace": {
      "$resolver": "exception",
      "field": "stackTrace",
      "stackTrace": {
        "stringified": true
      }
    }
  },
  "line_number": {
    "$resolver": "source",
    "field": "lineNumber"
  },
  "class": {
    "$resolver": "source",
    "field": "className"
  },
  "@version": 1,
  "source_host": "${hostName}",
  "message": {
    "$resolver": "message",
    "stringified": true
  },
  "thread_name": {
    "$resolver": "thread",
    "field": "name"
  },
  "@timestamp": {
    "$resolver": "timestamp"
  },
  "level": {
    "$resolver": "level",
    "field": "name"
  },
  "file": {
    "$resolver": "source",
    "field": "fileName"
  },
  "method": {
    "$resolver": "source",
    "field": "methodName"
  },
  "logger_name": {
    "$resolver": "logger",
    "field": "name"
  }
}
```
