package com.example.restservice;

import com.tersesystems.echopraxia.Condition;
import com.tersesystems.echopraxia.Logger;
import com.tersesystems.echopraxia.LoggerFactory;
import com.tersesystems.echopraxia.scripting.ScriptCondition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                HttpServletRequest request =
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .getRequest();
                return fb.requestFields(request);
              });

  private final Condition debugCondition = ScriptCondition.create(false,
            Paths.get("condition.tf"),
            e -> logger.error("Script failed!", e));

  private final Logger<HttpRequestFieldBuilder> debugLogger = logger.withCondition(debugCondition);

  @GetMapping("/greeting")
  public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
    logger.info("Greetings {}", fb -> fb.onlyString("greeting_name", name));

    debugLogger.debug("This message only shows up when request_remote_addr is 127.0.0.1 and level>=DEBUG");

    return new Greeting(counter.incrementAndGet(), String.format(template, name));
  }

}
