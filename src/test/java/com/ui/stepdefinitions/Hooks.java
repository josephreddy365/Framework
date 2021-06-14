package com.ui.stepdefinitions;

import com.utils.ReadProperties;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;

public class Hooks {
    public static WebDriver driver = null;
    public static String Url = ReadProperties.returnConfig("URL_env");
    static String url = "http://automationpractice.com/";

    @Before
    public void openBrowser(){
        System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/Browsers/Chrome/chromedriver.exe");
        driver = new ChromeDriver();
        driver.get(url);
    }

    @After(order = 0)
    public void closeBrowser(){
    driver.close();
    driver.quit();
    }

    @After(order = 1)
    public void embedScreenshot(io.cucumber.java.Scenario scenario) {

        if (scenario.isFailed()) {
            try {
                TakesScreenshot screenshot = (TakesScreenshot) driver;
                scenario.embed(screenshot.getScreenshotAs(OutputType.BYTES), "image/png", "Latest Screenshot Failure");

                if (System.getProperty("sel.saveScreenshot") != null) {
                    //log.info("Execution had sel.saveScreenshot parameter, storing screenshot");
                    File screenshotOut = new File(
                            String.format(
                                    "target/screenshots/%s.png",
                                    scenario.getName()
                            ));

                    if (screenshotOut.exists()) {
                        //log.info(String.format("Screenshot with name %s exists already, grabbing it and attaching it to report", screenshotOut.getName()));
                        scenario.embed(
                                FileUtils.readFileToByteArray(screenshotOut),
                                "image/png",
                                "First Screenshot Failure"
                        );

                        //log.info(String.format("Deleting screenshot with name %s from the disk - It has been embedded", screenshotOut.getName()));
                        FileUtils.deleteQuietly(screenshotOut);
                    } else {
                        //log.info(String.format("Saving screenshot with name %s to disk", screenshotOut.getName()));
                        FileUtils.copyFile(screenshot.getScreenshotAs(OutputType.FILE), screenshotOut);
                    }

                }

            } catch (WebDriverException wde) {
                //log.debug(wde.getMessage());
            } catch (ClassCastException | IOException cce) {
                cce.printStackTrace();
            }
        }
    }


    @After(order = 2)
    public void embedPageSource(io.cucumber.java.Scenario scenario) {

        if (scenario.isFailed()) {
            try {
                scenario.embed(driver.getPageSource().getBytes(), "text/plain");
            } catch (Exception ignored) {
            }
        }
    }
}
