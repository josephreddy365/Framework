package com.ui.stepdefinitions;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class Hooks {
    public static WebDriver driver = null;
    static String url = "http://automationpractice.com/";

    @Before
    public void openBrowser(){
        System.setProperty("webdriver.chrome.driver", "D:\\Apps and Drivers\\Selenium Drivers\\Driver Chrome\\chromedriver.exe");
        driver = new ChromeDriver();
        driver.get(url);
    }

    @After
    public void closeBrowser(){
    driver.close();
    driver.quit();
    }
}
