package com.example.restservice;

import com.tersesystems.echopraxia.Logger;
import com.tersesystems.echopraxia.LoggerFactory;
import com.tersesystems.echopraxia.LoggerHandle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@RestController
public class GreetingController {

  private static final String template = "Hello, %s!";
  private final AtomicLong counter = new AtomicLong();

  private final Logger<HttpRequestFieldBuilder> logger =
      LoggerFactory.getLogger(getClass())
          .withFieldBuilder(HttpRequestFieldBuilder.class)
          .withFields(
              fb -> {
                // Any fields that you set in context you can set conditions on later,
                // i.e. on the URI path, content type, or extra headers.
                HttpServletRequest request =
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .getRequest();
                return fb.requestFields(request);
              });

  // Creates a debug logger that will filter out any requests that doesn't meet the condition.
  private final Logger<HttpRequestFieldBuilder> debugLogger =
      logger.withCondition(Conditions.debugCondition);

  // Can also log asynchronously in a different thread if the condition is expensive
  private final AsyncLogger asyncLogger = AsyncLoggerFactory.getLogger(debugLogger.core(), debugLogger.fieldBuilder());

  @GetMapping("/greeting")
  public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
    // Log using a field builder to add a greeting_name field to JSON
    logger.info("Greetings {}", fb -> fb.onlyString("greeting_name", name));

    // the logger must be set to DEBUG level and also meet the condition.
    debugLogger.debug(
        "This message only shows up when request_remote_addr is 127.0.0.1 and level>=DEBUG");

    // async logger runs in a different thread pool
    asyncLogger.debug(wrap(h -> h.log("Same, but logs asynchronously")));

    return new Greeting(counter.incrementAndGet(), String.format(template, name));
  }

  private Consumer<LoggerHandle<HttpRequestFieldBuilder>> wrap(
      Consumer<LoggerHandle<HttpRequestFieldBuilder>> c) {
    // Because this takes place in the fork-join common pool, we need to set request
    // attributes in the thread before logging so we can get request fields.
    // See below link for alternatives to a method wrap:
    // https://medium.com/asyncparadigm/logging-in-a-multithreaded-environment-and-with-completablefuture-construct-using-mdc-1c34c691cef0
    final RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
    return h -> {
      try {
        RequestContextHolder.setRequestAttributes(requestAttributes);
        c.accept(h);
      } finally {
        RequestContextHolder.resetRequestAttributes();
      }
    };
  }
}
