package com.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class HomePage extends BasePage {

    @FindBy(id = "email")
    public static WebElement usernameTextBox;
    @FindBy(id = "passwd")
    public static WebElement passwordTextBox;
    @FindBy(id = "SubmitLogin")
    public static WebElement SignInButton;



    public static void getHomePage() {
        driver.get("http://automationpractice.com/index.php?controller=authentication&back=my-account");

    }
    public static void enterUsernameAndPassword(String username, String password) {
        usernameTextBox.sendKeys(username);
        passwordTextBox.sendKeys(password);
    }

    public static void clickSignInButton() {
        SignInButton.click();
    }


    public static void iSeeAnyTextOnThePage(String message) {
        driver.findElement(By.xpath("//*[contains(text(),'"+message+"')]"));
    }
}
