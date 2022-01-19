package com.example.restservice;

import com.tersesystems.echopraxia.Condition;
import com.tersesystems.echopraxia.Logger;
import com.tersesystems.echopraxia.LoggerFactory;
import com.tersesystems.echopraxia.scripting.ScriptCondition;
import com.tersesystems.echopraxia.scripting.ScriptHandle;
import com.tersesystems.echopraxia.scripting.ScriptWatchService;

import java.nio.file.Path;
import java.nio.file.Paths;

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
