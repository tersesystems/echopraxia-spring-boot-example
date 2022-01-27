# Echopraxia Spring Boot Example

This is a sample Spring Boot application that shows structured logging using [Echopraxia](https://github.com/tersesystems/echopraxia).

## Gradle

First, we add the logstash implementation of Echopraxia to `build.gradle`:

```groovy
dependencies {
	implementation 'com.tersesystems.echopraxia:logstash:1.1.3'
    implementation 'com.tersesystems.echopraxia:scripting:1.1.3'

    // typically you also want the latest version of logstash-logback-encoder as well..
    implementation 'net.logstash.logback:logstash-logback-encoder:7.0.1'
}
```

for Log4J, you will need to exclude the `spring-boot-starter-logging` implementation, and then add `spring-boot-starter-log4j2` and `spring-boot-starter-web` explicitly:

```groovy
configurations {
    implementation.exclude module: 'spring-boot-starter-logging'
}

dependencies {
	implementation 'com.tersesystems.echopraxia:log4j:1.1.3'
	implementation 'com.tersesystems.echopraxia:scripting:1.1.3'

	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.boot:spring-boot-starter-log4j2'
	implementation 'org.apache.logging.log4j:log4j-layout-template-json:2.17.1'
	
	testImplementation('org.springframework.boot:spring-boot-starter-test')
}
```

## GreetingController

The greeting controller is where the logger is created.  

Here, we'll set up a custom field builder that can extract elements out of an HTTP request, then use it to ensure that contextual data is extracted when logging.

We first do this by calling for a `Logger` just as you would for an SLF4J logger

```java
private final Logger<HttpRequestFieldBuilder> logger = LoggerFactory.getLogger(getClass()).withFieldBuilder(HttpRequestFieldBuilder.class)
```

We'll then add a function that will get access to the HTTP request using the `withFields` method:

```java
private final Logger<HttpRequestFieldBuilder> logger = LoggerFactory.getLogger(getClass())
    .withFieldBuilder(HttpRequestFieldBuilder.class)
    .withFields(fb -> {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .getRequest();
            return fb.requestFields(request);
    });
```

We can also add conditions that allow us to debug code with targeted debugging statements.  

```java
public class Conditions {

    private static final Logger logger = LoggerFactory.getLogger();

    // This should generally be global to the application, as it creates a watcher thread internally
    private static final Path scriptDirectory = Paths.get("scripts").toAbsolutePath();

    // Watch the directory
    private static final ScriptWatchService scriptWatchService = new ScriptWatchService(scriptDirectory);

    private static final ScriptHandle scriptHandle = scriptWatchService.watchScript(
            scriptDirectory.resolve("condition.tf"), e -> logger.error(e.getMessage(), e));

    // Creates a condition from a script and re-evaluates it whenever the script changes
    public static final Condition debugCondition = ScriptCondition.create(scriptHandle);
}
```

Here, we'll set up a debug logger based off the original logger:

```java
private final Logger<HttpRequestFieldBuilder> debugLogger = logger.withCondition(Conditions.debugCondition);
```

This will connect to a [Tweakflow](https://twineworks.github.io/tweakflow/) script that can evaluate context fields passed in.  In this case, we only want to return true if the remote address starts with `127`:

```
library echopraxia {

  # level: the logging level
  # fields: the dictionary of fields
  #
  function evaluate: (string level, dict fields) ->
    fields[:request_remote_addr] != nil && str.starts_with?(fields[:request_remote_addr], "127");
}
```

Using a script is very useful for debugging as you can change conditions in the script on the fly while your Spring Boot application is running, and the script manager will detect and recompile the script for you.

Here's the whole `GreetingController` code:

```java
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    private final Logger<HttpRequestFieldBuilder> logger =
            LoggerFactory.getLogger(getClass())
                    .withFieldBuilder(HttpRequestFieldBuilder.class)
                    .withFields(
                            fb -> {
                                HttpServletRequest request =
                                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                                                .getRequest();
                                return fb.requestFields(request);
                            });

    // Creates a debug logger that will filter out any requests that doesn't meet the condition.
    private final Logger<HttpRequestFieldBuilder> debugLogger = logger.withCondition(Conditions.debugCondition);

    @GetMapping("/greeting")
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        logger.info("Greetings {}", fb -> fb.onlyString("greeting_name", name));

        // the logger must be set to DEBUG level and also meet the condition.
        debugLogger.debug(
                "This message only shows up when request_remote_addr is 127.0.0.1 and level>=DEBUG");

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

## Spring Boot Logging

There are two configurations available, Logback and Log4J.

### Logback

The implementation is done through `logback-spring.xml`:

```xml
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <property name="LOG_FILE" value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}/}spring.log}"/>

    <include resource="org/springframework/boot/logging/logback/console-appender.xml" />

    <!-- make file contain JSON structured logging -->
    <include resource="json-file-appender.xml" />

    <logger name="com.example.restservice" level="DEBUG"/>

    <root level="INFO">
      <appender-ref ref="CONSOLE" />
      <appender-ref ref="FILE" />
  </root>
</configuration>
```

With the contents of `json-file-appender.xml`:

```xml
<included>
    <!-- use the default spring boot conventions here, but leverage a different encoder -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <!-- https://github.com/logfellow/logstash-logback-encoder -->
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
          <jsonGeneratorDecorator class="net.logstash.logback.decorate.PrettyPrintingJsonGeneratorDecorator"/>
        </encoder>

        <file>${LOG_FILE}</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz}</fileNamePattern>
            <cleanHistoryOnStart>${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}</cleanHistoryOnStart>
            <maxFileSize>${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}</maxFileSize>
            <totalSizeCap>${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}</totalSizeCap>
            <maxHistory>${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}</maxHistory>
        </rollingPolicy>
    </appender>
</included>
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

For Log4J, the implementation is in `log4j-spring.xml` with a packages pointing to `com.tersesystems.echopraxia.log4j.layout`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN" packages="com.tersesystems.echopraxia.log4j.layout">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT" follow="true">
            <JsonTemplateLayout eventTemplateUri="classpath:LogstashJsonEventLayoutCustom.json"/>
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
