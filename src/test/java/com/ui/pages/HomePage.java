package com.ui.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class HomePage extends BasePage {
    @FindBy(id = "")
    public static WebElement elementName;



    public static void getHomePage() {
        driver.get("http://automationpractice.com/index.php");
    }
}
