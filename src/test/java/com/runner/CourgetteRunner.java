package com.runner;

import courgette.api.CourgetteOptions;
import courgette.api.CourgetteRunLevel;
import courgette.api.CucumberOptions;
import courgette.api.testng.TestNGCourgette;
import org.testng.annotations.Test;


@Test
@CourgetteOptions(
        threads = 2,
        runLevel = CourgetteRunLevel.FEATURE,
        rerunFailedScenarios = false,
        rerunAttempts = 1,
        showTestOutput = true,
        reportTargetDir = "build",
        plugin = {"extentreports"},
        cucumberOptions = @CucumberOptions(
                features = {"src/test/resources/Features"},
                glue = {"com.ui.stepdefinitions"},
                tags = {"@test"},
                plugin = {"pretty",
                        "json:build/cucumber-report/cucumber.json",
                        "html:build/cucumber-report/cucumber.html"},
                strict = true
        ))
public class CourgetteRunner extends TestNGCourgette {
}
