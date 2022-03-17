package com.example.restservice;

import com.tersesystems.echopraxia.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicLong;

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
                // These fields will be visible in the JSON file, not shown in console.
                return fb.requestFields(httpServletRequest());
              });

  @NotNull
  private HttpServletRequest httpServletRequest() {
    return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
  }

  // For an async logger, we need to set thread local context if we have fields that depend on it
  private final AsyncLogger<?> asyncLogger = AsyncLoggerFactory.getLogger()
    .withFieldBuilder(HttpRequestFieldBuilder.class)
    .withThreadLocal(() -> {
    // get the request attributes in rendering thread...
    final RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
    // ...and the "set" in the runnable will be called in the logging executor's thread
    return () -> RequestContextHolder.setRequestAttributes(requestAttributes);
  }).withFields(fb -> fb.requestFields(httpServletRequest()));

  @GetMapping("/greeting")
  public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
    // Log using a field builder to add a greeting_name field to JSON
    logger.info("Greetings {}", fb -> fb.onlyString("greeting_name", name));

    asyncLogger.info("this message is logged in a different thread");

    // for async logger, if blocks don't work very well, instead use a handle method
    asyncLogger.info(h -> {
      // execution in this block takes place in the logger's thread
      h.log("Complex logging statement goes here");
    });

    return new Greeting(counter.incrementAndGet(), String.format(template, name));
  }
}
