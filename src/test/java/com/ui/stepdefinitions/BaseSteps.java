package com.ui.stepdefinitions;

import com.ui.pages.HomePage;
import com.ui.pages.SignInPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class BaseSteps {
    public WebDriver driver = Hooks.driver;
    public void BaseSteps(){
        PageFactory.initElements(driver, HomePage.class);
        PageFactory.initElements(driver, SignInPage.class);
    }
}



