package com.ui.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

public class CookieFooterModal extends BasePage {
    private static Logger log = LogManager.getLogger(CookieFooterModal.class);

    @FindBy(className = "cc-banner")
    public static WebElement cookieFooterBanner;

    @FindBy(className = "cc-window")
    public static WebElement cookieFooterWindow;

    @FindBy(className = "cc-link")
    public static WebElement learnMoreCookieLink;

    @FindBy(className = "cc-invisible")
    public static List<WebElement> invisibleCookieFooter;

    public static boolean cookieFooterDisplayed(){
        log.info("Checking if Cookie Footer is displayed");
        return BasePage.checkIfElementDisplayed(cookieFooterBanner);
    }

    public static void clickLearnMoreCookieLink() throws Exception {
        waitAndClickWithRetry(learnMoreCookieLink);
        log.info("Clicked on Learn More Link");
        openTab("latest");
        log.info("Switched to tab" + Driver.getTitle());
    }

    public static void clickGotItButton() throws Exception {
        waitForPageToLoad();
        WebElement gotItButton = Driver.findElement(By.className("cc-dismiss"));
        waitAndClickWithRetry(gotItButton);
        log.info("Clicked on Got it button");
        waitForElementToBeInvisible(cookieFooterBanner);
    }
    public static void waitForCookieFooterToBeVisible(){
        log.info("Waiting for Cookie footer window => " + cookieFooterWindow + " to be visible.");
        BasePage.waitForElementToBeVisible(cookieFooterWindow);
    }
}
