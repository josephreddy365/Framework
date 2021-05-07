package com.ui.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SignInPage extends BasePage{
    @FindBy(id = "")
    public static WebElement emailAddressTextBox;
    @FindBy(xpath = "")
    public static WebElement passwordTextBox;
    @FindBy(xpath = "")
    public static WebElement loginButton;

    public static void enterLoginCredentials(String email,String password){

    }


}
