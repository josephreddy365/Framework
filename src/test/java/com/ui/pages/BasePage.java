package com.ui.pages;

import com.google.common.base.Function;
import com.ui.dataproviders.ConfigFileReader;
import com.ui.dataproviders.ExcelReader;
import com.ui.stepdefinitions.Hooks;
import com.utils.ReadProperties;
import io.cucumber.datatable.DataTable;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.ui.util.CookieHelper.dismissCookieFooter;


public class BasePage {
    static ExcelReader Excel_Reader = new ExcelReader(System.getProperty("user.dir")+"/Configsfiles/TestData.xlsm");
    public static ConfigFileReader Config = new ConfigFileReader();
    static String Email = "Email";
    private static By initialProductMenuRootPath = By.xpath("//div[contains(@class, 'menuWrapper') and not(ancestor::li[@class='parent']) and not(ancestor::nav[contains(@class, 'customMobileMenu')])]/ul");
    public static WebDriver Driver ;
    private static Logger log = LogManager.getLogger(BasePage.class);
    public static String Env = null ;
    //Set timeOutDefault for WebDriverWait
    private static final int timeOutDefault = 30;
    //Set implicit wait for driver
    private static final int implicitWaitDefault = 15;
    //Initialise WebDriverWait variable
    private static WebDriverWait wait;
    //boolean to state whether the default implicit wait is set
    private static boolean isImplicitWaitSet = false;


    // DataTable constants
    static final String PROPERTY = "property";
    static final String ATTRIBUTE = "attribute";
    static final String VALUE = "value";

    //constructor
    public BasePage(){
            Driver =Hooks.driver;
        if(wait == null) setWebDriverWait();
        if(!isImplicitWaitSet) applyDefaultImplicitWait();
    }

    public static void setWebDriverWait() {
        BasePage.wait = new WebDriverWait(Driver, timeOutDefault);
    }

    //methods
    public static String getURL() {
        ConfigFileReader Config = new ConfigFileReader();
        String url = Config.ConfigFileReader("URL");
        if (Env==null)
        {
            Env = url;
        }
        log.info("URL from config is :"+ Env);
        return Env;
    }

    public static void waitForElementStaleness(WebElement Element) {
        wait.until(ExpectedConditions.refreshed(ExpectedConditions.stalenessOf(Element)));
    }

    public static void waitForElementToBeClickable(WebElement element) {
        new FluentWait<>(Driver)
                .withTimeout(Duration.ofSeconds(timeOutDefault))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class)
                .until(ExpectedConditions.elementToBeClickable(element));
    }

    public static void waitForElementToBeVisible(By element) {
        wait = new WebDriverWait(Driver, 40);
        wait.ignoring(StaleElementReferenceException.class);
        wait.until(ExpectedConditions.visibilityOf(Driver.findElement(element)));
        setWebDriverWait();
    }

    public static void waitForTextToBePresent(String expectedOption, WebElement element) {
        log.info("Waiting for text to be present in element " + element);
        wait = new WebDriverWait(Driver, 10);
        wait.ignoring(StaleElementReferenceException.class);
        log.info("Waiting for text: " + element.getText() + " to include: " + expectedOption);
        wait.until(ExpectedConditions.textToBePresentInElement(element, expectedOption));
    }

    public static void waitForElementToBeSelected(WebElement element) {
        log.info("Waiting for element =>" + element + "<= to be selected.");
        wait = new WebDriverWait(Driver, 15);
        wait.ignoring(StaleElementReferenceException.class);
        wait.until(ExpectedConditions.elementSelectionStateToBe(element, true));
        setWebDriverWait();
    }

    public static void waitForElementToBeVisible(WebElement element) {
        waitForElementToBeVisible(element, true);
    }

    public static void waitForElementToBeVisible(WebElement element, boolean ignoreStaleElement) {
        setWebDriverWait();
        if (ignoreStaleElement) {
            wait.ignoring(StaleElementReferenceException.class);
        }
        wait.until(ExpectedConditions.visibilityOf(element));
    }

    public static void waitForElementToBeStale(WebElement element) {
        wait.ignoring(StaleElementReferenceException.class);
        wait.until(ExpectedConditions.stalenessOf(Driver.findElement(extractByFromWebElement(element))));
    }

    public static void waitForElementToBeEnabled(By by) throws Exception {
        wait.ignoring(StaleElementReferenceException.class);
        wait.until(Driver -> {
            try {
                return !checkIfElementIsDisabled(waitForVisibleElement(by));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public static void waitForElementToBeEnabled(WebElement element) throws Exception {
        wait.ignoring(StaleElementReferenceException.class);
        wait.until(Driver -> !checkIfElementIsDisabled(element));
    }

    public static WebElement waitForOneOf(int secondsToWait, WebElement... elements) throws InterruptedException {
        log.info("Looking for one of [" + elements + "]");
        removeImplicitWait();
        int retries = 0;
        int msBetweenChecks = 200;
        while (retries < ((secondsToWait*1000)/msBetweenChecks)) {
            for (WebElement element : elements) {
                try {
                    if (checkIfElementDisplayed(element, true)) {
                        log.info("Found element =>" + element + "<= was displayed, returning element");
                        applyDefaultImplicitWait();
                        return element;
                    }
                } catch (StaleElementReferenceException e) {
                    log.info("Element was stale, attempting to check display using By");
                    element = Driver.findElement(extractByFromWebElement(element));
                    if (checkIfElementDisplayed(element, true)) {
                        log.info("Found element =>" + element + "<= was displayed, returning element");
                        applyDefaultImplicitWait();
                        return element;
                    }
                }
            }
            retries++;
            Thread.sleep(msBetweenChecks);
        }
        throw new NoSuchElementException("None of " + elements + " became visible on page");
    }

    public static WebElement waitForOneOf(int secondsToWait, By... bys) throws InterruptedException {
        int retries = 0;
        int msBetweenChecks = 200;
        log.info("Waiting for element at one of: " + bys);
        while (retries < (secondsToWait*1000)/msBetweenChecks) {
            for (By by : bys) {
                WebElement element;
                try {
                    element = getVisibleElement(by);
                } catch (RuntimeException e) {
                    log.info("Found multiple visible elements at By, so ignoring results: " + by);
                    continue;
                }
                if (element != null && checkIfElementDisplayed(element)) {
                    log.info("Got element at =>" + by + "<= at attempt #" + (retries + 1));
                    return element;
                }
            }
            retries++;
            Thread.sleep(msBetweenChecks);
        }
        throw new NoSuchElementException("None of " + bys + " became visible on page");
    }

    public static void waitForElementToBeInvisible(WebElement Element) {
        log.info("Waiting for invisibility of element  ==>" + Element + "<==");
        try {
            wait.until(ExpectedConditions.invisibilityOf(Element));
        } catch (NullPointerException i) {
            log.info("No elements were visible. Not waiting");
        }
    }

    public static void waitForElementToBeInvisible(By by) {
        log.info("Waiting for invisibility of element at By ==>" + by + "<==");
        try {
            wait.until(ExpectedConditions.invisibilityOf(getVisibleElement(by, true)));
        } catch (NullPointerException i) {
            log.info("No elements were visible. Not waiting");
        }
    }

    public static void waitForElementToBeVisibleAndClickable(WebElement Element) {
        waitForElementToBeVisible(Element);
        waitForElementToBeClickable(Element);
    }

    public static void waitForAttributeToBeLoaded(WebElement Element, String Attribute, String Value){
        wait.until(ExpectedConditions.attributeContains(Element, Attribute, Value));
    }

    public static void waitForLoad(WebDriver Driver) {
        ExpectedCondition<Boolean> pageLoadCondition = new ExpectedCondition<Boolean>()
        {
            public Boolean apply(WebDriver Driver)
            {
                return ((JavascriptExecutor)Driver).executeScript("return document.readyState").equals("complete");
            }
        };
        wait.until(pageLoadCondition);
    }

    public static void waitForDriver() {
        applyDefaultImplicitWait();
        waitForPageToLoad();
    }

    /**
     * Remove the implicit wait if the method is specifically designed so that an element may or may not exist
     * but is not critical to the goal of the method/test
     */
    public static void removeImplicitWait() {
        Driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        log.info("Removed Implicit wait. Implicit wait is now set to 0");
        isImplicitWaitSet = false;
    }

    /**
     * Apply the default Implicit wait to the driver in seconds,
     * this should only be called within the BaePage constructor or if a method hs overridden/removed the implicit wait
     */
    public static void applyDefaultImplicitWait() {
        if (isImplicitWaitSet) {
            log.debug("Implicit wait already set, not updating");
            return;
        }
        Driver.manage().timeouts().implicitlyWait(implicitWaitDefault, TimeUnit.SECONDS);
        log.info("Applied default implicit wait. Implicit wait is set to " + implicitWaitDefault + " seconds.");
        isImplicitWaitSet = true;
    }

    public static void scrollToElement(By by, boolean elementShouldBeTop) {
        JavascriptExecutor je = (JavascriptExecutor) Driver;
        je.executeScript("arguments[0].scrollIntoView(" + elementShouldBeTop + ");", Driver.findElement(by));
    }

    public static void scrollToElement(WebElement element, boolean elementShouldBeTop) {
        JavascriptExecutor je = (JavascriptExecutor) Driver;
        je.executeScript("arguments[0].scrollIntoView(" + elementShouldBeTop + ");", element);
    }

    public static void scrollToElement(final WebDriver Driver, final WebElement Element){
        JavascriptExecutor je = (JavascriptExecutor) Driver;
        je.executeScript("arguments[0].scrollIntoView();",Element);
    }

    public static void scriptClickElement(final WebDriver Driver, final WebElement Element) throws Exception {
        JavascriptExecutor je = (JavascriptExecutor) Driver;
        je.executeScript("arguments[0].click();",Element);
    }

    public static void doubleClick(WebElement Element) {
        Actions actions = new Actions(Driver);
        actions.doubleClick(Element).perform();
    }

    public static void screenshot(WebDriver Driver,String File_With_Path) throws IOException {

        TakesScreenshot scrShot =((TakesScreenshot)Driver);
        File SrcFile=scrShot.getScreenshotAs(OutputType.FILE);
        File DestFile=new File(File_With_Path);
        FileUtils.copyFile(SrcFile, DestFile);

    }

    @SuppressWarnings("deprecation")
    public static WebElement waitToFindElement(final WebDriver Driver, final WebElement Element, final int Timeout_Seconds) {
        FluentWait<WebDriver> wait = new FluentWait<WebDriver>(Driver)
                .withTimeout(Timeout_Seconds, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(NoSuchElementException.class);


        return wait.until(new Function<WebDriver, WebElement>() {
            public WebElement apply(WebDriver webDriver) {
                return Element;
            }
        });
    }

    public static void scrollToTop (final WebDriver Driver) {
        JavascriptExecutor jse = (JavascriptExecutor) Driver;
        jse.executeScript("scroll(0, -250);");
    }

    public static void scrollToBottom (final WebDriver Driver)
    {
        JavascriptExecutor jse = (JavascriptExecutor) Driver;
        jse.executeScript("scroll(0, 2500);");
    }

    public static void moveToWebElement(WebElement element) {
        Actions actions = new Actions(Driver);
        actions.moveToElement(element).build().perform();
    }

    private static void waitAndMove(WebElement element) {
        waitAndMove(element, true);
    }

    private static void waitAndMove(WebElement element, boolean ignoreStaleElement) {
        waitForElementToBeVisible(element, ignoreStaleElement);
        moveToWebElement(element);
    }

    public static void waitAndMoveWithRetry(WebElement element) {
        waitAndMoveWithRetry(element, false);
    }

    public static void waitAndMoveWithRetry(WebElement element, boolean elementByIsSafeToFindBy) {
        try {
            waitAndMove(element, false);
        } catch (StaleElementReferenceException e) {
            e.printStackTrace();
            log.debug("Element has become stale, retrying...");
            if (elementByIsSafeToFindBy) element = Driver.findElement(extractByFromWebElement(element));
            else Driver.navigate().refresh();
            waitAndMove(element);
        } catch(TimeoutException e) {
            e.printStackTrace();
            log.debug("Timeout Exception occurred, retrying...");
            waitForPageToLoad();
            waitAndMove(element);
        } catch (ElementNotVisibleException e) {
            e.printStackTrace();
            log.debug("Element is not visible, retrying...");
            waitForPageToLoad();
            waitAndMove(element);
        }
    }

    public static void selectDropdownByValue(WebElement Element, String Value) {
        log.info("Going to select dropdown value =>" + Value);
        Select select = new Select(Element);
        select.selectByValue(Value);
    }
    public static void selectDropDownByText(WebElement element, String text) {
        Select select = new Select(element);
        select.selectByVisibleText(text);
    }

    public static void selectDropDownByIndex(WebElement element, String index, boolean ignoreDiscontinuedOptions)  {
        int i = Integer.parseInt(index);
        Select select = new Select(element);
        while (ignoreDiscontinuedOptions && select.getOptions().get(i).getText().toLowerCase().contains("discontinued")) i++;
        select.selectByIndex(i);
    }

    private static void waitAndClick(WebElement element) throws Exception {
        try {
            element.click();
        } catch (StaleElementReferenceException e) {
            log.info(element + " has become stale...");
            waitForVisibleElement(extractByFromWebElement(element)).click();
            log.info("Successfully re-clicked " + element + " following refresh");
        } catch (NoSuchElementException e){
            waitForPageToLoad();
            waitForElementToBeClickable(element);
            element.click();
        }
    }


    /**
     * Retry if the element has encountered a stale element exception or Timeout Exception,
     * attempt a moveToElement and retry when it is not visible,
     * otherwise an exception is thrown
     * <p>
     * Override default WebDriverWait to be 15 seconds
     */
    public static void waitAndClickWithRetry(WebElement element) throws Exception {
        waitAndClickWithRetry(element, false);
    }

    public static void waitAndClickWithRetry(WebElement element, boolean elementByIsSafeForFindBy) throws Exception {
        wait = new WebDriverWait(Driver, 15);
        applyDefaultImplicitWait();
        try {
            waitAndClick(element);
            try {
                removeImplicitWait();
                log.debug("Clicked on element =>" + element);
                applyDefaultImplicitWait();
            } catch (WebDriverException e) {
                applyDefaultImplicitWait();
                log.debug("Clicked on element within iFrame - continuing without error");
            }
        } catch (TimeoutException e) {
            log.info("Timeout Exception occurred, retrying...");
            waitForPageToLoad();
            log.info("Temporarily increasing WebDriver wait to 40 seconds");
            wait = new WebDriverWait(Driver, 40);
            waitAndClick(element);
            log.info("Successfully re-clicked element");
            setWebDriverWait();
        } catch (ElementNotVisibleException e) {
            log.info("Element is not visible, attempting move an click...");
            waitForPageToLoad();
            waitForElementToBeVisible(element);
            waitAndMoveWithRetry(element, elementByIsSafeForFindBy);
            waitAndClick(element);
            log.info("Succesfully moved to element and clicked");
        } catch (ElementNotInteractableException e) {
            log.info("Element Click Intercepted occurred." +
                    "Ensure that \"" + element + "\" is visible and has been moved to in order to avoid this exception." +
                    "Attempting move to element and retrying click");
            waitForPageToLoad();
            element = elementByIsSafeForFindBy ? Driver.findElement(extractByFromWebElement(element)) : element;
            scrollToElement(Driver, element);
            try {
                waitAndClick(element);
            } catch (ElementClickInterceptedException e1) {
                log.warn("Element click was intercepted, scrolling element to bottom and retrying");
                scrollToElement(element, false);
                waitAndClick(element);
            }
            log.info("Successfully re-clicked element");
        }
        setWebDriverWait();
        waitForDriver();
    }

    public static void waitAndEnterText(WebElement element, String text) throws Exception {
        try {
            log.info("Attempting to clear and send =>" + text + "<= to =>" + element);
            element.clear();
            element.sendKeys(text);
        } catch (StaleElementReferenceException e){
            log.info(element + "<= was stale. Waiting for it to be visible");
            element = waitForVisibleElement(extractByFromWebElement(element));
            log.info("Attempt #2 about to clear and send =>" + text + "<= to =>" + element);
            element.clear();
            element.sendKeys(text);
        }
    }

    public static boolean waitToSeeIfElementBecomesDisplayed(By by) throws InterruptedException {
        log.info("Waiting to see if element =>" + by + "<= becomes displayed.");
        for (int retries = 0; retries < 10; retries++) {
            log.info("Attempt #" + (retries + 1));
            if (checkIfElementDisplayed(by)) return true;
            Thread.sleep(300);
        }
        return false;
    }

    /**
     * @param element pass in type WebElement to check whether it is displayed on the page
     * @return true if displayed and false if not displayed
     * Temporarily remove the implicit wait for this check and reapply when found or No Such Element is caught
     * Catch StaleElementException and check for element using extracted By
     */
    public static boolean checkIfElementDisplayed(WebElement element) {
        return checkIfElementDisplayed(element, false);
    }

    public static boolean checkIfElementDisplayed(WebElement element, boolean callerRemovesImplicitWait) {
        log.info("Going to check if element =>" + element + "<= is displayed");
        if (!callerRemovesImplicitWait) removeImplicitWait();
        try {
            boolean displayed = element.isDisplayed();
            log.info("Found element: " + displayed + " that element is displayed.");
            if (!callerRemovesImplicitWait) applyDefaultImplicitWait();
            return displayed;
        } catch (NoSuchElementException e) {
            log.debug("No such element found, returning false.");
            if (!callerRemovesImplicitWait) applyDefaultImplicitWait();
            return false;
        } catch (StaleElementReferenceException | TimeoutException e) {
            log.debug("Element was Stale, retrying...");
            By by = extractByFromWebElement(element);
            try {
                boolean displayed = Driver.findElement(by).isDisplayed();
                log.info("Found element: " + displayed + " that element is displayed.");
                if (!callerRemovesImplicitWait) applyDefaultImplicitWait();
                return displayed;
            } catch (NoSuchElementException e1) {
                log.debug("No such element found, returning false.");
                if (!callerRemovesImplicitWait) applyDefaultImplicitWait();
                return false;
            }
        }


    }

    /**
     * @param by pass in type By to check whether WebElement located at By is displayed on the page
     * @return true if displayed and false if not displayed
     * Temporarily remove the implicit wait for this check and reapply when found or No Such Element is caught
     * Catch StaleElementException and recheck
     */
    public static boolean checkIfElementDisplayed(By by) {
        log.info("Going to check if element =>" + by + "<= is displayed");
        removeImplicitWait();
        try {
            boolean displayed = checkIfElementAtByIsDisplayed(by);
            log.info("Found element: " + displayed + " that element is displayed.");
            applyDefaultImplicitWait();
            return displayed;
        } catch (NoSuchElementException e) {
            log.debug("No such element found, returning false.");
            applyDefaultImplicitWait();
            return false;
        } catch (StaleElementReferenceException | TimeoutException e) {
            log.debug("Element was Stale, retrying...");
            try {
                boolean displayed = Driver.findElement(by).isDisplayed();
                log.info("Found element: " + displayed + " that element is displayed.");
                applyDefaultImplicitWait();
                return displayed;
            } catch (NoSuchElementException e1) {
                log.debug("No such element found, returning false.");
                applyDefaultImplicitWait();
                return false;
            }
        }
    }

    public static boolean checkIfElementDisplayed(By... bys) {
        log.info("Checking if element at one of " + bys + " is displayed");
        for (By by : bys) {
            log.info("Looking for element at =>" + by);
            if (checkIfElementDisplayed(by)) {
                log.info("Found element displayed, returning true");
                return true;
            }
        }
        log.info("Did not find displayed element at any checked By, returning false");
        return false;
    }

    private static boolean checkIfElementAtByIsDisplayed(By by) {
        List<WebElement> elements = Driver.findElements(by);
        for (WebElement element : elements) if (element.isDisplayed()) return true;
        return false;
    }

    public static By getDisplayedBy(By... potentialBys) {
        for (By by : potentialBys) {
            if (checkIfElementDisplayed(by)) {
                log.info(by + " was displayed");
                return by;
            }
            log.info(by + " was not displayed; continuing through list");
        }
        throw new NoSuchElementException("None of the parameter Bys were displayed: " + potentialBys);
    }

    public static By waitForDisplayedBy(int secondsToWait, By... potentialBys) {
        int retries = 0;
        while (retries < secondsToWait) {
            try {
                return getDisplayedBy(potentialBys);
            } catch (NoSuchElementException e) {
                log.warn("Did not find any By displayed: " + e);
                retries++;
            }
        }
        return getDisplayedBy(potentialBys);
    }

    /**
     * @param element pass in type WebElement to extract By from String
     * @return By of type used to find element to begin
     * Should not be passed chained elements, or elements found via findElements
     */
    public static By extractByFromWebElement(WebElement element) {
        log.info("Attempting to retrieve By from WebElement: " + element.toString());
        String byType;
        String byString;

        if (element.toString().contains("Proxy element")) {
            log.info("WebElement has Page-Element Proxy-type By");
            byType = StringUtils.substringBetween(element.toString(), "By.", ":");
        } else {
            log.info("Found generic By");
            byType = StringUtils.substringBetween(element.toString(), "-> ", ":");
        }
        byString = StringUtils.substringAfter(element.toString(), byType + ": ");
        byString = byString.substring(0, byString.length() - 1);

        log.info("Found By of type \"" + byType + "\" with content: \"" + byString + "\"");

        switch (byType) {
            case "xpath":
                return By.xpath(byString);
            case "css selector":
            case "cssSelector":
                return By.cssSelector(byString);
            case "id":
                return By.id(byString);
            case "class name":
            case "className":
                return By.className(byString);
            case "link text":
            case "linkText":
                return By.linkText(byString);
            case "partial link text":
            case "partialLinkText":
                return By.partialLinkText(byString);
            case "name":
                return By.name(byString);
            case "tag name":
            case "tagName":
                return By.tagName(byString);
            default:
                log.warn("Could not find corresponding By for \"" + byType + "\"");
                throw new IllegalArgumentException(byType + " is not an implemented By type.");
        }
    }

    public static boolean checkIfElementExists(List<WebElement> elementList) {
        return elementList.size() > 0;
    }

    public static boolean checkIfElementExists(WebElement root, By element) {
        removeImplicitWait();
        boolean exists;
        try {
            root.findElement(element);
            exists = true;
        } catch (NoSuchElementException e) {
            log.debug("No element found: " + e);
            exists = false;
        }
        log.info("Was " + exists + " that element at By " + element + " exists under root " + root);
        applyDefaultImplicitWait();
        return exists;
    }

    public static boolean checkIfElementExists(By element) {
        removeImplicitWait();
        boolean exists;
        try {
            Driver.findElement(element);
            exists = true;
        } catch (NoSuchElementException e) {
            log.debug("No element found: " + e);
            exists = false;
        }
        log.info("Was " + exists + " that element at By " + element + " exists");
        applyDefaultImplicitWait();
        return exists;
    }

    public static void waitUntilElementExist(List<WebElement> elementList){
        try{
            log.info("waiting for at least one " + elementList.toString() + " to exist");
            wait.until(Driver -> elementList.size() > 0);
        }
        catch (TimeoutException e){
            log.debug(elementList + " does not exist on the page after waiting");
            e.printStackTrace();
        }
    }

    public static String getDataFromExcelReader(String sheet, String columnTitle, int row) {
        String excelCellData = Excel_Reader.getCellData(sheet, columnTitle, row);
        log.debug("Data from Excel Spreadsheet: '" + columnTitle + "' returns '" + excelCellData + "'");
        return excelCellData;
    }

    /**
     * Return true if waitForElementToHaveText does not raise an exception
     * This method call also checks for visibility of element
     *
     * @param text
     * @return
     */
    public static boolean messageWithTextIsVisible(String text) {
        waitForElementToHaveText(By.tagName("body"), text);
        return true;
    }

    public static void waitForAdditionalElement(int existingElements, By locator) {
        log.info("Waiting for more than " + existingElements + " to be located at \"" + locator + "\"");
        wait.until(Driver -> Driver.findElements(locator).size() > existingElements);
    }

    public static Boolean urlContainsText(String partialUrl){
        return Driver.getCurrentUrl().toLowerCase().contains(partialUrl);
    }


    /**
     * Opens Product Dropdown before moving to each element in turn, and clicking the final element.
     * Retrieves a By for each String parameter and locates the corresponding element.
     * Retry if the element has encountered a stale element exception or Timeout Exception,
     * 	retry if encounter an ElementClickInterceptedException, otherwise
     *
     * @throws Exception
     * @param {String} takes up to 3 Strings corresponding to each level of the menu
     *
     * 		Override default WebDriverWait to be 15 seconds
     */
    public static void navigateProductDropdownWithRetry(String menuLevel1) throws Exception {
        wait = new WebDriverWait(Driver, 15);
        try {
            navigateProductDropdown(menuLevel1);
        } catch (StaleElementReferenceException e) {
            e.printStackTrace();
            log.debug("Element has become stale, retrying...");
            Driver.navigate().refresh();
            navigateProductDropdown(menuLevel1);
        } catch (TimeoutException e) {
            e.printStackTrace();
            log.debug("Timeout Exception occurred, retrying...");
            waitForPageToLoad();
            navigateProductDropdown(menuLevel1);
        } catch (ElementNotVisibleException e) {
            e.printStackTrace();
            log.debug("Element is not visible, retrying...");
            waitForPageToLoad();
            navigateProductDropdown(menuLevel1);
        } catch (ElementClickInterceptedException e) {
            log.debug("Element Click Intercepted occurred. " +
                    "Dismissing cookie footer, retrying...");
            waitForPageToLoad();
            //Check that the Cookie footer is not intercepting the click
            navigateProductDropdown(menuLevel1);
        }
        setWebDriverWait();
    }

    public static void navigateProductDropdownWithRetry(String menuLevel1, String menuLevel2) throws Exception {
        wait = new WebDriverWait(Driver, 15);
        try {
            navigateProductDropdown(menuLevel1, menuLevel2);
        } catch (StaleElementReferenceException e) {
            e.printStackTrace();
            log.debug("Element has become stale, retrying...");
            Driver.navigate().refresh();
            navigateProductDropdown(menuLevel1, menuLevel2);
        } catch (TimeoutException e) {
            e.printStackTrace();
            log.debug("Timeout Exception occurred, retrying...");
            waitForPageToLoad();
            navigateProductDropdown(menuLevel1, menuLevel2);
        } catch (ElementNotVisibleException e) {
            e.printStackTrace();
            log.debug("Element is not visible, retrying...");
            waitForPageToLoad();
            navigateProductDropdown(menuLevel1, menuLevel2);
        } catch (ElementClickInterceptedException e) {
            log.debug("Element Click Intercepted occurred. " +
                    "Dismissing cookie footer, retrying...");
            waitForPageToLoad();
            //Check that the Cookie footer is not intercepting the click
            dismissCookieFooter();
            navigateProductDropdown(menuLevel1, menuLevel2);
        }
        setWebDriverWait();
    }

    public static void navigateProductDropdownWithRetry(String menuLevel1, String menuLevel2, String menuLevel3) throws Exception {
        wait = new WebDriverWait(Driver, 15);
        try {
            navigateProductDropdown(menuLevel1, menuLevel2, menuLevel3);
        } catch (StaleElementReferenceException e) {
            e.printStackTrace();
            log.debug("Element has become stale, retrying...");
            Driver.navigate().refresh();
            navigateProductDropdown(menuLevel1, menuLevel2, menuLevel3);
        } catch (TimeoutException e) {
            e.printStackTrace();
            log.debug("Timeout Exception occurred, retrying...");
            waitForPageToLoad();
            navigateProductDropdown(menuLevel1, menuLevel2, menuLevel3);
        } catch (ElementNotVisibleException e) {
            e.printStackTrace();
            log.debug("Element is not visible, retrying...");
            waitForPageToLoad();
            navigateProductDropdown(menuLevel1, menuLevel2, menuLevel3);
        } catch (ElementClickInterceptedException e) {
            log.debug("Element Click Intercepted occurred. " +
                    "Dismissing cookie footer, retrying...");
            waitForPageToLoad();
            //Check that the Cookie footer is not intercepting the click
            dismissCookieFooter();
            navigateProductDropdown(menuLevel1, menuLevel2, menuLevel3);
        }
        setWebDriverWait();
    }

    private static void navigateProductDropdown(String menuLevel1) throws Exception {
        waitAndMoveWithRetry(waitForVisibleElement(By.cssSelector("li[class*='productsMenu'] > a[title='Products']")));

        log.info("Setting root to menu container");
        WebElement productMenuRoot = waitForVisibleElement(initialProductMenuRootPath);
        log.info("Waiting for Element by title =>" + menuLevel1 + "<= to be visible");
        WebElement nextElement = waitForVisibleElement(getByTitle(replaceAllNonAlphaNumeric(menuLevel1)), productMenuRoot);
        log.info("Found Element by title '" + menuLevel1 + "'. Moving to final Element and clicking.");
        waitAndMove(nextElement);
        nextElement.click();
    }

    private static void navigateProductDropdown(String menuLevel1, String menuLevel2) throws Exception {
        waitAndMoveWithRetry(waitForVisibleElement(By.cssSelector("li[class*='productsMenu'] > a[title='Products']")));

        log.info("Setting root to menu container");
        WebElement productMenuRoot = waitForVisibleElement(initialProductMenuRootPath);
        log.info("Waiting for Element by title =>" + menuLevel1 + "<= to be visible");
        WebElement nextElement = waitForVisibleElement(getByTitle(replaceAllNonAlphaNumeric(menuLevel1)), productMenuRoot);
        log.info("Found Element by title '" + menuLevel1 + "'. Moving to Element.");
        waitAndMove(nextElement);

        log.info("Setting root to second level menu container which is related to the above selected WebElement");
        productMenuRoot = getProductSubMenuRoot(menuLevel1);
        log.info("Waiting for Element by title =>" + menuLevel2 + "<= to be visible");
        nextElement = waitForVisibleElement(getByTitle(replaceAllNonAlphaNumeric(menuLevel2)), productMenuRoot);
        log.info("Found Element by title '" + menuLevel2 + "'. Moving to final Element and clicking.");
        waitAndMove(nextElement);
        nextElement.click();
    }

    private static void navigateProductDropdown(String menuLevel1, String menuLevel2, String menuLevel3) throws Exception {
        waitAndMoveWithRetry(waitForVisibleElement(By.cssSelector("li[class*='productsMenu'] > a[title='Products']")));

        log.info("Setting root to menu container");
        WebElement productMenuRoot = waitForVisibleElement(initialProductMenuRootPath);
        log.info("Waiting for Element by title =>" + menuLevel1 + "<= to be visible");
        WebElement nextElement = waitForVisibleElement(getByTitle(replaceAllNonAlphaNumeric(menuLevel1)), productMenuRoot);
        log.info("Found Element by title '" + menuLevel1 + "'. Moving to Element.");
        waitAndMove(nextElement);

        log.info("Setting root to second level menu container which is related to the above selected WebElement");
        productMenuRoot = getProductSubMenuRoot(menuLevel1);
        log.info("Waiting for Element by title =>" + menuLevel2 + "<= to be visible");
        nextElement = waitForVisibleElement(getByTitle(replaceAllNonAlphaNumeric(menuLevel2)), productMenuRoot);
        log.info("Found Element by title '" + menuLevel2 + "'. Moving to Element.");
        waitAndMove(nextElement);

        log.info("Setting root to third level menu container which is related to the above selected WebElement");
        productMenuRoot = getProductSubMenuSecond(menuLevel2);
        log.info("Waiting for Element by title =>" + menuLevel3 + "<= to be visible");
        nextElement = waitForVisibleElement(getByTitle(replaceAllNonAlphaNumeric(menuLevel3)), productMenuRoot);
        log.info("Found Element by title '" + menuLevel3 + "'. Moving to final Element and clicking.");
        waitAndMove(nextElement);
        nextElement.click();

    }
    public static String replaceAllNonAlphaNumeric(String input) {
        input = input.replaceAll("[^0-9A-Za-z ]", "");
        log.info("Final product dropdown title was \"" + input + "\"");
        return input;
    }

    public static WebElement waitForVisibleElement(By by) throws Exception {
        return waitForVisibleElement(by, false);
    }

    public static WebElement waitForVisibleElement(By by, boolean allowMultiple) throws Exception {

        int retries = 0;
        int msBetweenRetries = 300;
        WebElement visibleElement = getVisibleElement(by, allowMultiple);

        removeImplicitWait();
        while (visibleElement == null && retries <= 60) {
            retries++;
            Thread.sleep(msBetweenRetries);
            try {
                visibleElement = getVisibleElement(by, allowMultiple);
            } catch (StaleElementReferenceException ignored) {	}
        }
        applyDefaultImplicitWait();
        if (visibleElement == null) {
            log.debug("Did not get any elements from list when expected to return 1 visible element");
            throw new Exception("No elements became visible. " +
                    "Tried for " + (retries * msBetweenRetries) / 1000 + "seconds.");
        } else {
            return visibleElement;
        }
    }

    public static WebElement waitForVisibleElement(By by, WebElement root) throws Exception {

        int retries = 0;
        int msBetweenRetries = 200;
        WebElement visibleElement = getVisibleElement(by, root);

        removeImplicitWait();
        while (visibleElement == null && retries <= 200) {
            retries++;
            Thread.sleep(msBetweenRetries);
            try {
                visibleElement = getVisibleElement(by, root);
            } catch (StaleElementReferenceException ignored) {	}
        }

        applyDefaultImplicitWait();

        if (visibleElement == null) {
            log.debug("Did not get any elements from list when expected to return 1 visible element");
            throw new Exception("No elements with By => " + by + " became visible. " +
                    "Tried for " + (retries * msBetweenRetries) / 1000 + "seconds.");
        } else {
            return visibleElement;
        }
    }

    public static WebElement getVisibleElement(By by) {
        return getVisibleElement(by, false);
    }

    private static WebElement getVisibleElement(By by, boolean allowMultiple) {
        removeImplicitWait();
        int count = 0;
        WebElement uniqueElement = null;
        List<WebElement> elements = Driver.findElements(by);
        log.info("Found =>" + elements.size() + "<= elements with by =>" + by + "<= Going to see which ones are visible");
        if (elements.size() == 1)
            try {
                return elements.get(0).isDisplayed() ? elements.get(0) : null;
            } catch (StaleElementReferenceException e) {
                log.info("Received Stale Element after finding 1 visible element with by =>" + by + "<= Sending back null");
                return null;
            }

        for (WebElement element : elements) {
            try {
                if (element.isDisplayed()) {
                    count += 1;
                    if (uniqueElement == null) uniqueElement = element;
                }
            } catch (Exception e) {
                log.debug("Received error when checking whether element was displayed");
                e.printStackTrace();
            }
        }

        applyDefaultImplicitWait();

        if (count == 0) {
            log.debug("Did not get any elements from list when expected to return 1 visible element");
            return null;
        } else if (count > 1 && !allowMultiple) {
            throw new RuntimeException("Got multiple displayed elements. I saw =>"+elements.size()+"<= of which visible =>"+count);
        }
        log.debug("Received '" + elements.size() + "' elements to check and returned one visible element");
        return uniqueElement;

    }
    public static List<WebElement> waitForVisibleElements(By by) throws Exception {

        int count = 0;

        while (count < 20){
            List<WebElement> visibleElements = getVisibleElements(by);
            if (visibleElements.size() > 0) {
                log.info("Returning =>" + visibleElements.size() + "<= visible elements for by =>" + by);
                return visibleElements;
            }
            Thread.sleep(500);
            count++;
        }

        throw new RuntimeException("Could not find any visible elements for =>"+by);
    }

    private static List<WebElement> getVisibleElements(By by) {
        log.info("Looking for visible elements by by =>" + by);
        List<WebElement> visibleElements = new ArrayList<>();
        List<WebElement> foundElements = Driver.findElements(by);

        log.info("Found =>" + foundElements.size() + "<= elements by by =>" + by + "<= Going to return visible elements");
        removeImplicitWait();
        for (WebElement element : foundElements) {
            if(element.isDisplayed()){
                visibleElements.add(element);
            }
        }
        log.info("Found =>" + visibleElements.size() + "<= visible elements by by =>" + by);

        return visibleElements;
    }

    private static WebElement getVisibleElement(By by, WebElement root) {
        int count = 0;
        WebElement uniqueElement = null;
        List<WebElement> elements = root.findElements(by);
        if (elements.size() == 1)
            return elements.get(0).isDisplayed() ? elements.get(0) : null;

        for (WebElement element : elements) {
            try {
                if (element.isDisplayed()) {
                    count += 1;
                    if (uniqueElement == null) uniqueElement = element;
                }
            } catch (Exception e) {
                log.debug("Received error when checking whether element was displayed");
                e.printStackTrace();
            }
        }

        if (count == 0) {
            log.debug("Did not get any elements from list when expected to return 1 visible element");
            return null;
        } else if (count > 1) {
            throw new RuntimeException("Got multiple displayed elements");
        }
        log.debug("Received '" + elements.size() + "' elements to check and returned one visible element");
        return uniqueElement;

    }

    public static WebElement getNthChildFromDropdown(WebElement dropdown, int n) throws Exception {
        log.info("Looking for " + n + "th child of \"" + dropdown + "\"");
        return waitForVisibleElement(By.cssSelector(String.format("li:nth-child(%d)", n)), dropdown);
    }

    private static WebElement getProductSubMenuRoot(String subMenu) throws Exception {
        subMenu = subMenu.trim();
        return waitForVisibleElement(By.xpath(String.format("//div[" +
                        "contains(@class, \"menu-subcats\") " +
                        "and preceding-sibling::*/a[@title=\"%s\"] " +
                        "and not(ancestor::nav[contains(@class, \"customMobileMenu\")])]/div",
                subMenu)));
    }

    private static WebElement getProductSubMenuSecond(String subMenu) throws Exception {
        subMenu = subMenu.trim();
        return waitForVisibleElement(By.xpath(String.format("//div[contains(@class, \"subcat-level-1\") and a[@title=\"%s\"]]", subMenu)));
    }

    public static By getByTitle(String title) {
        return By.xpath(String.format(".//a[@title=\"%s\"]", title));
    }

    public static By getByIndex(int index) {
        return By.xpath(String.format("(//div[@class='img-sqr'])[%d]", index));
    }

    public static By getByText(String text) {
        return By.xpath(String.format("//*[contains(text(),\"%s\")]", text));
    }

    public static String replaceSpacesAndDotsInStringWithHyphens(String str){
        log.info("Return product name as a string split with hyphens.");
        return str.replace(" ", "-").replace(".", "-");
    }



    public static void increaseQuantity(WebElement element, int quantity) throws Exception {
        log.info("Attempting to increase by " + quantity + "...");
        for (int i = 0; i < quantity; i++)
            waitAndClickWithRetry(element);
        log.info("Successfully increased quantity.");
    }

    public static void waitAndAssertForTextBy(By element,String expectedText) throws InterruptedException {
        String elementDisplayedText = null;
        int i=0;
        int maxTry=90;

        log.info("Going to look for element passed with text =>" + expectedText);

        while(i<maxTry) {
            elementDisplayedText=Driver.findElement(element).getText();
            log.info("Going to look for element attempt number"+i+" with text:" + elementDisplayedText);

            if(!StringUtils.isEmpty(elementDisplayedText)) {
                log.info("Got element text at attempt number"+i+" with text:" + elementDisplayedText);
                break;
            }
            Thread.sleep(500);
            i++;
        }
        log.info("Element displayed Text is: "+ elementDisplayedText);
        log.info("Expected text is :" +expectedText);
        assertWithMessage("Time Limit Text assertion has failed after =>"+maxTry+"<= attempts").that(elementDisplayedText).contains(expectedText);
    }

    public static void openTab(String tab) throws Exception {
        LinkedList<String> tabs = new LinkedList<String>(Driver.getWindowHandles());
        log.info("Switching to tab =>" + tabs.getLast());
        switch (tab) {
            case "latest":
                Driver.switchTo().window(tabs.getLast());
                break;
            case "first":
                Driver.switchTo().window(tabs.getFirst());
                break;
            default:
                throw new Exception("not a recognised tab argument");
        }
        waitForDriver();
    }

    public static Boolean waitForPageToLoad() {
        WebDriverWait wait = new WebDriverWait(Driver, 30);
        JavascriptExecutor js = (JavascriptExecutor) Driver;

        log.info("Waiting for JS/Jquery to load.....");

        ExpectedCondition<Boolean> jQueryLoad = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    return ((Long)js.executeScript("return jQuery.active") == 0);
                }
                catch (Exception e) {
                    return false;
                }
            }
        };
        try {
            if (wait.until(jQueryLoad)) {
                log.info("Page has finished loading, continuing");
                return true;
            } else {
                log.warn("Did not see jquery returning as active... continuing without error");
                return false;
            }
        } catch (TimeoutException e) {
            log.warn("Received timeout exception waiting for JQuery to load... continuing without error");
            return false;
        }
    }


    public static void clickOnLinkText(String linkText) throws Exception {
        log.info("Going to click on URL with link text =>"+linkText);
        WebElement linkTextElement;

        try {
            removeImplicitWait();
            linkTextElement = Driver.findElement(By.linkText(linkText));
        } catch (Exception e) {
            log.info("Could not find element link text, attempting search by contains text =>"+linkText);
            applyDefaultImplicitWait();
            linkTextElement = Driver.findElement(By.xpath("//*[text()[contains(., \"" + linkText + "\")]]|//*[@alt=\"" + linkText + "\"]"));
        }
        BasePage.waitAndClickWithRetry(linkTextElement);
        log.info("Clicked on URL with link text =>" + linkText);
    }
    /**
     * Below method can be used when a text on UI should be asserted in
     * a particular section of a webpage and text is present on multiple places
     * @param {expectedTextOnUI} Text to be asserted on UI
     * @param {elementClassName} Class Name of the element in which text is present
     * 	Below method can be amended to add more identifiers if required
     */
    public static void assertTextIsVisible(String expectedTextOnUI, String elementClassName) throws InterruptedException {
        String elementDisplayedText = null;
        int i = 0;
        int maxTry = 30;

        log.info("Going to look for element passed with text =>" + expectedTextOnUI);

        while (i < maxTry) {
            elementDisplayedText = Driver.findElement(By.className("" + elementClassName + "")).getText();
            log.info("Going to look for element attempt number" + i + " with text:" + elementDisplayedText);

            if (!StringUtils.isEmpty(elementDisplayedText)) {
                log.info("Got element text at attempt number" + i + " with text:" + elementDisplayedText);
                break;
            }
            Thread.sleep(500);
            i++;
        }
        log.info("Element display Text is: "+ elementDisplayedText);
        assertWithMessage("Expected Text => "+expectedTextOnUI+ " is missing").that(elementDisplayedText).contains(expectedTextOnUI);
    }

    public static void assertTextIsVisibleInModal(String expectedTextOnUI, WebElement element) throws InterruptedException {
        log.info("Waiting for =>" + element + "<= to contain =>" + expectedTextOnUI);
        wait.until(ExpectedConditions.textToBePresentInElement(element, expectedTextOnUI));
        assertThat(getMessageFromModal(element)).contains(expectedTextOnUI);
    }

    public static void assertTextIsVisibleInElement(String expectedTextOnUI, WebElement element) throws InterruptedException {
        log.info("Waiting for =>" + element + "<= to contain =>" + expectedTextOnUI);
        wait.until(ExpectedConditions.textToBePresentInElement(element, expectedTextOnUI));
        assertThat(element.getText()).contains(expectedTextOnUI);
    }

    public static void assertTextStartsWithIsVisibleInElement(String expectedTextOnUI, WebElement element) throws InterruptedException {
        log.info("Waiting for =>" + element + "<= to contain =>" + expectedTextOnUI);
        wait.until(ExpectedConditions.textToBePresentInElement(element, expectedTextOnUI));
        assertThat(element.getText()).startsWith(expectedTextOnUI);
    }

    public static WebElement getElementByText(String text) {
        return Driver.findElement(By.xpath("//*[text()=\"" + text + "\"]"));

    }

    public static String getAttribute(WebElement webElement, String attribute){
        return webElement.getAttribute(attribute);
    }

    public static WebElement getInputWebElementbyLabelName(String fieldName) throws InterruptedException {
        fieldName = fieldName.toLowerCase();
        log.info("Looking for input where label is \"" + fieldName + "\"");
        By inputByLocation = By.xpath(String.format("//label[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"%s\"]/following-sibling::input", fieldName.toLowerCase()));
        By inputByFor = By.xpath("//input[@id=//label[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"" + fieldName + "\"]/@for]");
        By inputByInputName = By.xpath(String.format("//*[translate(normalize-space(@id), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"%s\"]", fieldName.toLowerCase()));
        return waitForOneOf(30, inputByFor, inputByLocation, inputByInputName);
    }

    public static void waitForElementToHaveText(By by, String text) {
        log.info("Waiting to find text \"" + text + "\" in element at =>" + by);
        wait.ignoring(StaleElementReferenceException.class);
        wait.until(driver -> Driver.findElement(by).getText().toLowerCase().contains(text.toLowerCase()));
        wait.until(ExpectedConditions.visibilityOf(Driver.findElement(by)));
    }

    public static void navigateBack(){
        String previousUrl = Driver.getCurrentUrl();
        Driver.navigate().back();
        waitForPageToLoad();
        assertWithMessage("Url has not changed").that(Driver.getCurrentUrl().equals(previousUrl)).isFalse();
    }

    public static void refreshPage(){
        Driver.navigate().refresh();
        waitForPageToLoad();
    }

    public static void selectCheckBoxWithRetryScript(By by) throws Exception {
        log.info("Going to check checkbox for element with by =>"+by);
        WebElement checkbox = Driver.findElement(by);
        int attempts = 0;

        while (attempts < 5) {
            checkbox = Driver.findElement(by);
            if (checkbox.isSelected()) {
                log.info("Checkbox by by =>" + by + "<= is selected, continuing");
                return;
            }

            log.info("Checkbox is unchecked, checking now: attempt #" + (attempts + 1));
            scriptClickElement(Driver, checkbox);
            log.info("Script clicked checkbox");


            Thread.sleep(1000);
            attempts++;

        }

        if (checkbox.isSelected())
            log.info("Checked checkbox successfully");
        else {
            log.warn("Could not select checkbox");
            throw new RuntimeException("Selenium failed to select checkbox");
        }
    }

    public static void unselectCheckBoxWithRetryScript(By by) throws Exception {
        log.info("Going to uncheck checkbox for element with by =>"+by);
        WebElement checkbox;
        int attempts = 0;

        while (attempts < 5) {
            checkbox = Driver.findElement(by);
            if (!checkbox.isSelected()) {
                log.info("Checkbox by by =>" + by + "<= is unselected, continuing");
                return;
            }
            log.info("Checkbox is checked, unchecking now: attempt #" + (attempts + 1));
            scriptClickElement(Driver, checkbox);
            log.info("Script clicked checkbox");

            Thread.sleep(1000);
            attempts++;
        }

        checkbox = Driver.findElement(by);
        if (!checkbox.isSelected())
            log.info("Unchecked checkbox successfully");
        else {
            log.warn("Could not unselect checkbox");
            throw new RuntimeException("Selenium failed to unselect checkbox");
        }
    }

    public static void selectCheckBoxWithRetry(By by) throws Exception {
        log.info("Looking for element to select at By: " + by);
        selectCheckBoxWithRetry(waitForVisibleElement(by), true);
    }

    public static void unselectCheckBoxWithRetry(By by) throws Exception {
        log.info("Looking for element to deselect at By: " + by);
        selectCheckBoxWithRetry(waitForVisibleElement(by), false);
    }

    public static void selectCheckBoxWithRetry(WebElement checkbox) throws Exception {
        selectCheckBoxWithRetry(checkbox, true);
    }

    public static void selectCheckBoxWithRetry(WebElement checkbox, boolean shouldSelect) throws Exception {
        WebElement clickable = checkbox;
        log.info("Got checkbox tagname as: " + checkbox.getTagName());
        if (checkbox.getTagName().equals("label")) {
            log.info("Looking for equivalent input to assert selection");
            checkbox = Driver.findElement(By.xpath("//input[@id = '" + clickable.getAttribute("for") + "']"));
        } else if (checkbox.getTagName().equals("span")) {
            log.info("Looking for equivalent input to assert selection");
            checkbox = clickable.findElement(By.xpath("./input"));
        } else {
            log.info("Input is same as click location, continuing as normal");
        }
        performSelectCheckBoxWithRetry(checkbox, clickable, shouldSelect);
    }

    private static void performSelectCheckBoxWithRetry(WebElement checkbox, WebElement clickable, boolean shouldSelect) throws Exception {
        int attempts = 0;
        String un = shouldSelect ? "" : "un";
        while ((checkbox.isSelected() != shouldSelect) && attempts < 5) {
            log.info("Checkbox is not in expected state, " + un + "checking now: attempt #" + (attempts + 1));
            waitAndClickWithRetry(clickable);
            if (checkbox.isSelected() != shouldSelect){
                log.info("Checkbox selected state was " + checkbox.isSelected() + " when expected " + shouldSelect + "; waiting...");
                Thread.sleep(1000);
                attempts++;
            }
        }
        if (checkbox.isSelected() == shouldSelect)
            log.info(un + "checked checkbox successfully");
        else {
            log.warn("Could not " + un + "select checkbox");
            throw new RuntimeException("Selenium failed to " + un + "select checkbox");
        }
    }

    public static void clickOnCheckBoxLabel(String labelText) throws Exception {
        By checkboxLabel = By.xpath("//label[contains(text(),\"" + labelText + "\")]");
        waitAndClickWithRetry(waitForVisibleElement(checkboxLabel));
        WebElement checkboxLabelElement = Driver.findElement(checkboxLabel);
        assertWithMessage("Checkbox could not be checked =>"+labelText).that(Driver.findElement(By.id(checkboxLabelElement.getAttribute("for"))).isSelected()).isTrue();
    }
    public static void clickEllipsisButton() throws Exception {
        waitForPageToLoad();
        By ellipsisButtonBy = By.cssSelector(".glyphicon-option-vertical");
        log.info("Waiting for ellipsis button to be visible and enabled...");
        WebElement ellipsisButton = waitForVisibleElement(ellipsisButtonBy, true);
        waitForElementToBeEnabled(ellipsisButton);
        log.info("Ellipsis button is visible and enabled. Selecting the first available ellipsis");
        waitAndClickWithRetry(ellipsisButton);
    }


    public static void selectBreadcrumbByText(String breadcrumbText) throws Exception {
        log.info("Selecting " + breadcrumbText + " in the breadcrumb.");
        waitAndClickWithRetry(Driver.findElement(By.xpath(String.format("//div[contains(@class, 'paddingTop20 customerBreadcrumbStyle')]//strong[contains(text(), \"%1$s\")] | //ul[contains(@class,'breadcrumb')]/li/a[contains(text(), \"%1$s\")]", breadcrumbText))));
    }

    public static WebElement getModalWithTitleText(String modalTitle) throws Exception {
        return waitForOneOf(20, getByForModalWithTitleText(modalTitle), getByForCboxWithTitleText(modalTitle));
    }

    public static By getByForModalWithTitleText(String modalTitle) {
        return By.xpath("//div[contains(@class, 'modal-content') and contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'),\"" + modalTitle.toLowerCase() + "\")]");
    }

    public static By getByForCboxWithTitleText(String modalTitle) {
        return By.xpath("//div[contains(@id, 'cboxContent') and contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'),\"" + modalTitle.toLowerCase() + "\")]");
    }

    public static void searchTextInModalWithTitleText(String searchText, String titleText) throws Exception {
        waitForPageToLoad();
        log.info("Searching modal \"" + titleText + "\" for \"" + searchText + "\"");
        waitForVisibleElement(By.xpath("//div[contains(@class, 'modal-content') and contains(.,\"" + titleText + "\")]//input[@type='text']")).sendKeys(searchText);
    }

    public static Map<String, String> mapEmailResult(List<String> emailTable) {
        Map<String, String> email = new TreeMap<>();
        log.info("Parsing data from email table to map...");
        email.put("Subject", emailTable.get(0).trim());
        email.put("Recipient Name", emailTable.get(1).trim());
        email.put("Recipient", emailTable.get(2).trim());
        // Body should never be null, but nullcheck here allows run on dev1
        email.put("Body", emailTable.get(3) == null ? null : emailTable.get(3).trim());
        log.info("Got: " + email);
        return email;
    }

    public static String getMessageFromModal(WebElement modal) {
        return modal.findElement(By.cssSelector("div[class*='modal-body']")).getText().trim();
    }

    public static String setEmailForEnvironment(String email) {
        if (email.contains("+env")){
            String updatedEmail = email.replace("+env", "+"+ ReadProperties.ENVIRONMENT);
            log.info("Updating email from =>"+email+"<= to =>"+updatedEmail);
            return updatedEmail;
        }
        if (email.contains("+dev")){
            String updatedEmail = email.replace("+dev", "+"+ReadProperties.ENVIRONMENT);
            log.info("Updating email from =>"+email+"<= to =>"+updatedEmail);
            return updatedEmail;
        }

        log.info("Email =>" + email + "<= did not contain generic environment value for us to replace, returning with no change");
        return email;
    }

    public static void assertElementAttribute(String field, String attribute, String value) throws InterruptedException {
        log.info("Waiting for attribute to be loaded =>" + attribute + "<= from the field =>" + field + "<=");
        waitForAttributeToBeLoaded(BasePage.getInputWebElementbyLabelName(field), attribute, value);
        assertWithMessage("Expected field=>" + field +"<= has an attribute of =>" + attribute + "<= with a value of =>" + value + "<=").that(BasePage.getInputWebElementbyLabelName(field).getAttribute(attribute)).isEqualTo(value);
    }

    public static void assertElementAttributeById(String id, String attribute, String value) throws Exception {
        log.info("Waiting for attribute to be loaded =>" + attribute + "<= from the input element =>" + id + "<=");
        waitForAttributeToBeLoaded(waitForVisibleElement(By.cssSelector("input#" + id)), attribute, value);
        assertWithMessage("Expected field=>" + id +"<= has an attribute of =>" + attribute + "<= with a value of =>" + value + "<=").that(waitForVisibleElement(By.cssSelector("input#" + id)).getAttribute(attribute)).isEqualTo(value);
    }

    public static void waitForPageToChange(String title, String urlString) throws Exception {
        int i = 0;
        JavascriptExecutor js = (JavascriptExecutor) Driver;

        while (i <= 10) {
            log.info("Retry attempt =>" + i);
            log.info("Current Url =>" + Driver.getCurrentUrl());
            if (Driver.getCurrentUrl().contains(urlString)) {
                log.info("Returning from the method as url now contains =>" + urlString);
                return;
            } else {
                log.info("Going to click on title =>" + title);
                waitAndClickWithRetry(Driver.findElement(By.cssSelector("a[title='" + title + "']")));
            }
            log.info("document ready state =>" + js.executeScript("return document.readyState"));
            Thread.sleep(300);
            i++;
        }
    }

    public static void selectPageInSelectDropdown(String page, WebElement informationPageDropdown, List<WebElement> informationPageDropdownOptions, boolean requiresScriptClickToToggle) throws Exception {
        log.info("Opening dropdown and looking for option matching \"" + page + "\"");
        if (requiresScriptClickToToggle) {
            log.info("Opening dropdown using script click as toggle is not clickable via normal methods...");
            scriptClickElement(Driver, informationPageDropdown);
        } else {
            log.info("Opening dropdown using waitAndClickWithRetry...");
            waitAndClickWithRetry(informationPageDropdown);
        }
        log.info("Opened dropdown successfully");
        for (WebElement option : informationPageDropdownOptions) {
            log.info("Option is \"" + option.getText() + "\"");
            if (option.getText().toLowerCase().contains(page.toLowerCase())) {
                log.info("Found option matching \"" + page + "\", clicking element...");
                waitAndClickWithRetry(option);
                log.info("Clicked option successfully");
                return;
            }
        }
        throw new NoSuchElementException("Did not find option matching \"" + page + "\"");

    }

    public static void selectPageInSelectDropdown(String page, WebElement informationPageDropdown, List<WebElement> informationPageDropdownOptions) throws Exception {
        selectPageInSelectDropdown(page, informationPageDropdown, informationPageDropdownOptions, false);
    }

    public static void navigateClickBasedDropdownWithRetry(String optionText, WebElement dropdown, List<WebElement> dropdownOptions, boolean requiresScriptClickToToggle) throws Exception {
        try {
            log.info("Trying to select option in dropdown");
            selectPageInSelectDropdown(optionText, dropdown, dropdownOptions, requiresScriptClickToToggle);
        } catch (ElementClickInterceptedException | NoSuchElementException | StaleElementReferenceException | ElementNotVisibleException | TimeoutException e) {
            log.warn("Unable to select dropdown for option text =>" + optionText + "<= for dropdown element =>" + dropdown + "<= with dropdownOptions =>" + dropdownOptions);
            if (requiresScriptClickToToggle) {
                log.info("Opening dropdown using script click as toggle is not clickable via normal methods...");
                scriptClickElement(Driver, dropdown);
            } else {
                log.info("Opening dropdown using waitAndClickWithRetry...");
                waitAndClickWithRetry(dropdown);
            }
            By by = extractByFromWebElement(dropdownOptions.get(0));
            log.info("Extracted By =>" + by + "<= to find elements by for dropdown selection with text =>" + optionText);
            for (WebElement option : Driver.findElements(by)) {
                if (option.getText().toLowerCase().contains(optionText.toLowerCase())) {
                    log.info("Found option matching \"" + optionText + "\", clicking element...");
                    waitAndClickWithRetry(option);
                    return;
                }
            }
        }

    }

    public static void navigateClickBasedDropdownWithRetry(String optionText, WebElement dropdown, List<WebElement> dropdownOptions) throws Exception {
        navigateClickBasedDropdownWithRetry(optionText, dropdown, dropdownOptions,false);
    }

    public static void inputText(WebElement searchInput, String searchText) throws Exception {
        log.info("Sending text to element =>" + searchInput );
        waitForElementToBeVisibleAndClickable(searchInput);
        waitForElementToBeEnabled(searchInput);
        if (searchText.isEmpty()) {
            log.info("Planned text to input was blank, using backspace to delete current content");
            while (!searchInput.getAttribute("value").isEmpty()) {
                searchInput.sendKeys(Keys.BACK_SPACE);
            }
        } else {
            log.info("Clearing and inputting \"" + searchText + "\" to input");
            searchInput.clear();
            log.info("Entering text =>" + searchText);
            searchInput.sendKeys(searchText);
        }
    }

    public static void clearInputViaKeyboard(WebElement inputElement) {
        inputElement.sendKeys(Keys.chord(Keys.CONTROL, "a", Keys.DELETE));
    }

    public static void assertSearchResults(String expectedText, List<WebElement> results) {
        log.info("Asserting returned results contain searched text \"" + expectedText + "\"");
        for (WebElement result : results) {
            log.info("Checking row with text: " + result.getText());
            if (checkIfElementDisplayed(result)) assertThat(result.getText().toLowerCase()).contains(expectedText.toLowerCase());
        }
    }
    public static void selectFirstUnselectedAndVisibleRadioButton() throws Exception {
        log.info("Selecting the first visible and unselected radio button available");
        List<WebElement> radioButtons = Driver.findElements(By.cssSelector("input[type=radio]"));
        for(WebElement element: radioButtons){
            WebElement elementSib = element.findElement(By.xpath("following-sibling::label"));
            if(elementSib.isDisplayed() && !element.isSelected()) {
                log.info("Found a radio button displayed but not selected. Clicking clickable radio button label");
                waitAndClickWithRetry(elementSib);
                return;
            }
        }
        throw new NoSuchElementException("No unselected radio buttons are visible on the page");
    }

    public static void clickButtonWithTextWithinModal(String buttonText) throws Exception {
        log.info("Clicking for button in modal with text \"" + buttonText + "\"");
        BasePage.waitAndClickWithRetry(getButtonWithTextWithinModal(buttonText));
        waitForPageToLoad();
    }

    public static WebElement getButtonWithTextWithinModal(String buttonText) throws InterruptedException {
        log.info("Looking for button in modal with text \"" + buttonText + "\"");
        By modalBy = By.xpath(String.format("//div[contains(@class,\"modal-content\")]//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), \"%s\")]", buttonText.toLowerCase()));
        By cboxBy = By.xpath(String.format("//div[contains(@id,\"cbox\")]//button[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), \"%s\")]", buttonText.toLowerCase()));
        By modalLinkBy = By.xpath(String.format("//div[contains(@class,\"modal-content\")]//a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), \"%s\")]", buttonText.toLowerCase()));
        By cboxLinkBy = By.xpath(String.format("//div[contains(@id,\"cbox\")]//a[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), \"%s\")]", buttonText.toLowerCase()));
        return BasePage.waitForOneOf(20, modalBy, cboxBy, modalLinkBy, cboxLinkBy);
    }

    public static String getTodayDate(String dateFormat) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat));
        log.info("Today's date is " + date);
        return date;
    }
    public static String getDaysFromToday(int numberOfDays, String dateFormat) {
        String date = LocalDate.now().plusDays(numberOfDays).format(DateTimeFormatter.ofPattern(dateFormat));
        log.info("returned date is " + date);
        return date;
    }

    public static void switchToMainPage() {
        waitForPageToLoad();
        log.info("Switching out of iFrame");
        removeImplicitWait();
        Driver.switchTo().defaultContent();
        setWebDriverWait();
    }

    public static void assertListOfSortedBy(List<WebElement> list, String sortBy, boolean isReversed) {
        log.info("Asserting List " + sortBy + "s correctly sorted");

        switch (sortBy.toLowerCase()) {
            case "cardholder":
            case "card holder":
                log.info("Asserting names correctly ordered");
                List<String> names = list.stream().map(WebElement::getText).collect(Collectors.toList());
                log.info("Got list: " + names);
                assertListOrder(isReversed, names);
                break;
            case "expiry date":
            case "expiry":
                log.info("Asserting expiry dates correctly ordered");
                DateTimeFormatter mmyyyy = DateTimeFormatter.ofPattern("ddMM/yyyy");
                List<LocalDate> expiryDates = list.stream().map(element -> "01" + StringUtils.substringAfter(element.getText(), "Expires ")).map(dateString -> LocalDate.parse(dateString, mmyyyy)).collect(Collectors.toList());
                log.info("Got list: " + expiryDates);
                assertListOrder(isReversed, expiryDates);
                break;
            case "firstname":
                log.info("Asserting List correctly ordered By FirstName");
                List<String> firstNames = list.stream().map(WebElement::getText).collect(Collectors.toList());
                log.info("Got list: " + firstNames);
                assertListOrder(isReversed, firstNames);
                break;
            case "surname":
                log.info("Asserting List correctly ordered Surname");
                List<String> surnames = list.stream().map(WebElement::getText).collect(Collectors.toList());
                log.info("Got list: " + surnames);
                assertListOrder(isReversed, surnames);
                break;
            default:
                throw new IllegalArgumentException(sortBy + " is not a recognised/implemented sort");
        }
    }

    public static void assertListOrder(boolean isReversed, List<?> list) {
        assertThat(list).isOrdered((Comparator<String>) (o1, o2) -> {
            if (isReversed) return o2.compareToIgnoreCase(o1);
            else return o1.compareToIgnoreCase(o2);
        });
    }

    public static void assertListOfStringsSortedBy(List<String> list, String sortBy, boolean isReversed) {
        log.info("Asserting List " + sortBy + "s correctly sorted");

        if ("orgname".equals(sortBy.toLowerCase())) {
            log.info("Asserting List correctly ordered orgname");
            List<String> orgNames = list.stream().map(String::trim).collect(Collectors.toList());
            log.info("Got list: " + orgNames);
            assertListOrder(isReversed, orgNames);
        } else {
            throw new IllegalArgumentException(sortBy + " is not a recognised/implemented sort");
        }
    }

    public static void assertListOfSearch(List<WebElement> list, String columnName, String searchInput) {
        log.info("Asserting List only contains results containing: " + searchInput);

        switch (columnName.toLowerCase()) {
            case "firstname":
            case "surname":
            case "principalname":
            case "customerassignedname":
                log.info("Asserting List correctly filtered by " + searchInput);
                List<String> namesInColumn = list.stream().map(WebElement::getText).collect(Collectors.toList());
                log.info("Got list: " + namesInColumn);
                compareSearchInputToList(namesInColumn, searchInput);
                break;
            case "employeefirstname":
                log.info("Asserting List correctly filtered by customer Assigned Name search");
                List<String> employeeFirstNames = new ArrayList<>();
                for (WebElement e : list) {
                    employeeFirstNames.add(e.getAttribute("data-firstname"));
                }
                log.info("Got list: " + employeeFirstNames);
                compareSearchInputToList(employeeFirstNames, searchInput);
                break;
            case "employeesurname":
                List<String> employeeSurnames = new ArrayList<>();
                for (WebElement e : list) {
                    employeeSurnames.add(e.getAttribute("data-lastname"));
                }
                log.info("Got list: " + employeeSurnames);
                compareSearchInputToList(employeeSurnames, searchInput);
                break;
            default:
                throw new IllegalArgumentException(searchInput + " is not a recognised search");
        }
    }

    private static void compareSearchInputToList(List<String> list, String searchInput) {
        log.info("Checking whether " + searchInput + " is present in all strings in: " + list);
        boolean searchElementInList;
        for (int i = 0; i < list.size(); i++) {
            searchElementInList = list.get(i).contains(searchInput);
            if (searchElementInList) {
                log.info(list.get(i) + "contains: " + searchInput);
            } else {
                throw new AssertionError(list + " contains element not in " + searchInput);
            }
        }
        log.info(searchInput + " is contained in every List input in list: " + list);
    }

    public static void selectViewOnPDP(String view) throws Exception {
        waitAndClickWithRetry(Driver.findElement(By.xpath(String.format("//a[@id= '%s-button']", view.toLowerCase()))));
    }

    public static void navigateToUrl(String url) {
        log.info("Navigating to \"" + url + "\"");
        String finalUrl = parseUrlToFinalUrl(url);
        log.info("Got final url as ==>" + finalUrl + "<==");
        Driver.navigate().to(finalUrl);
    }

    private static String parseUrlToFinalUrl(String url) {
        log.info("Parsing url ==>" + url + "<== to final url");
        if (url.contains(".")) return url;
        else if (url.startsWith("/")) return Hooks.Url + StringUtils.substringAfter(url, "/");
        else return Hooks.Url + url;
    }

    public static boolean checkIfElementIsDisabled(WebElement element) {
        if (element.getAttribute("disabled") != null) {
            log.info("Element is disabled - contained attribute \"disabled\"");
            return true;
        } else if (element.getAttribute("class").contains("disabled")) {
            log.info("Element is disabled - class contained \"disabled\"");
            return true;
        } else if (element.findElement(By.xpath("..")).getAttribute("class").contains("disabled")) {
            log.info("Element is disabled - parent element class contained \"disabled\"");
            return true;
        } else if (element.findElement(By.xpath("..")).getAttribute("disabled") != null) {
            log.info("Element is disabled - parent element contained attribute \"disabled\"");
            return true;
        } else if (!element.isEnabled()) {
            log.info("Element is disabled - was not enabled");
            return true;
        }  else if (element.getCssValue("pointer-events").equalsIgnoreCase("none")) {
            log.info("Found applied css to disable element");
            return true;
        }
        log.info("Found no evidence that element disabled");
        return false;
    }

    public static void assertElementIsEnabledOrDisabledByText(String elementText, String elementType, String enabledOrDisabled) throws Exception {
        switch (elementType) {
            case "link":
                elementType = "a";
                break;
            case "list item":
                elementType = "li";
                break;
            case "checkbox":
                elementType = "input";
                break;
            case "label":
                elementType = "label";
                break;
            case "input":
            case "a":
            case "button":
            case "li":
            case "span":
                break;
            default:
                throw new Exception("Element type " + elementType + "not defined.");
        }

        if (enabledOrDisabled.equals("enabled")) {
            assertWithMessage(elementType + " with text \"" + elementText + "\" is not " + enabledOrDisabled)
                    .that(checkIfElementIsDisabled(getElementByExactText(elementType, elementText)))
                    .isFalse();
        } else {
            assertWithMessage(elementType + " with text \"" + elementText + "\" is not " + enabledOrDisabled)
                    .that(checkIfElementIsDisabled(getElementByExactText(elementType, elementText)))
                    .isTrue();
        }
        log.info(elementType + " is " + enabledOrDisabled);
    }

    public static void assertElementInListIsEnabledOrDisabledById(int position, String identifier, String enabledOrDisabled) throws Exception {
        List<WebElement> elementList = Driver.findElements(By.id(identifier));
        WebElement element = elementList.get(position);
        assertElementIsEnabledOrDisabled(element, enabledOrDisabled);
    }

    public static void assertSingleElementIsEnabledOrDisabledByID(String identifier, String enabledOrDisabled) throws Exception {
        WebElement element = Driver.findElement(By.id(identifier));
        assertElementIsEnabledOrDisabled(element, enabledOrDisabled);
    }

    public static void assertElementIsEnabledOrDisabled(WebElement element, String enabledOrDisabled) throws Exception {
        if (enabledOrDisabled.equals("enabled")) {
            assertWithMessage("Element \"" + element + "\" is not " + enabledOrDisabled)
                    .that(checkIfElementIsDisabled(element))
                    .isFalse();
        } else {
            assertWithMessage("Element \"" + element + "\" is not " + enabledOrDisabled)
                    .that(checkIfElementIsDisabled(element))
                    .isTrue();
        }
        log.info("Element is " + enabledOrDisabled);
    }

    public static WebElement getElementByExactText(String elementType, String exactElementText) throws Exception {
        log.info("Locating element where text is \"" + exactElementText + "\" - case insensitive & ignoring spacing");
        return BasePage.waitForOneOf(30, getByByExactText(elementType, exactElementText));
    }

    public static By[] getByByExactText(String elementType, String exactElementText) {
        log.info("Generating By where text is \"" + exactElementText + "\" - case insensitive & ignoring spacing");
        By exactBy = By.xpath(String.format("//%s[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"%s\"]", elementType, exactElementText.toLowerCase()));
        By innerBy = By.xpath(String.format("//%s[./*[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"%s\"]]", elementType, exactElementText.toLowerCase()));
        By[] bys = {exactBy, innerBy};
        return bys;
    }

    public static By getByByAltText(String elementType, String exactElementText) throws Exception {
        log.info("Generating By where text is \"" + exactElementText + "\" - case insensitive & ignoring spacing");
        return By.xpath(String.format("//%s[translate(normalize-space(@alt), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"%s\"]", elementType, exactElementText.toLowerCase()));
    }

    public static void userCanOrCannotAccessPage(String canOrCannot, String url) throws Exception {
        log.info("Deciding if the user should be able to access page");
        if(canOrCannot.equalsIgnoreCase("can")) {
            log.info("User should be able to access=>" + url + "<=");
            assertThat(Driver.getCurrentUrl()).contains(url);
            if(BasePage.checkIfElementDisplayed(By.xpath("//div[@class='container']/h4")))
            {
                // This will hit if the user does not have the correct permission
                throw new InvalidParameterException("Expected user to be authorised to view this page");
            }
        }
        else if(canOrCannot.equalsIgnoreCase("cannot"))
        {
            log.info("User should not be able to access=>" + url + "<= and should be prompted with an error message");
            assertThat(Driver.getCurrentUrl()).contains(url);
            WebElement errorContainer = Driver.findElement(By.xpath("//div[@class='container']/h4"));
            log.info("Created new web element to access expected error message=>" + errorContainer.getText() + "<=");
            assertThat(errorContainer.getText()).contains("Not authorised to view or perform action");
        }
        else {
            // We should never hit this
            throw new InvalidParameterException("Did not hit the if or else block, something went wrong");
        }
    }

    public static void assertElementWithIdHasTextWhenVisible(String elementId, String text) throws Exception {
        log.info("Asserting element #" + elementId + " contains text \"" + text + "\"");
        String actualText = waitForVisibleElement(By.cssSelector("#" + elementId)).getText();
        log.info("Got text as: \"" + actualText + "\"");
        assertThat(actualText).contains(text);
    }

    public static void assertUrl(String url) {
        log.info("Asserting that the specified url =>" + url + "<= is contained within the current");
        assertThat(Driver.getCurrentUrl()).contains(url);
        log.info("Logging current url =>" + Driver.getCurrentUrl() + "<=");
    }

    public static WebElement getClickableForCheckboxByLabel(String labelText, String precedingOrFollowing) throws InterruptedException {
        log.info("Retrieving clickable element for checkbox with label \"" + labelText + "\"");
        labelText = labelText.toLowerCase();
        By inputByLocation = By.xpath(String.format("//label[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"%s\"]/following-sibling::input", labelText.toLowerCase()));
        By altInputByLocation = By.xpath(String.format("//label[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"%s\"]/preceding-sibling::input", labelText.toLowerCase()));
        By inputByFor = By.xpath("//input[@id=//label[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"" + labelText + "\"]/@for]");
        By label = By.xpath("//label[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"" + labelText + "\"]");
        if ("preceding".equalsIgnoreCase(precedingOrFollowing)) {
            return waitForOneOf(30, altInputByLocation, inputByFor);
        } else if ("following".equalsIgnoreCase(precedingOrFollowing)) {
            return waitForOneOf(30, inputByLocation, inputByFor);
        } else {
            return waitForOneOf(30, inputByFor, label, inputByLocation, altInputByLocation);
        }
    }

    public static WebElement getClickableForCheckboxByLabel(String labelText) throws InterruptedException {
        return getClickableForCheckboxByLabel(labelText, null);
    }

    public static boolean isNumeric(String value) {
        log.info("checking if given input is numeric");
        if (value == null) {
            log.info("value was null, so not numeric");
            return false;
        } else {
            try {
                Double.parseDouble(value);
                log.info("the string \"" + value + "\" is numeric");
                return true;
            } catch (NumberFormatException e) {
                log.info("the string \"" + value + "\" is not numeric");
                return false;
            }
        }
    }

    public static List<WebElement> getDropdownOptionsForDropdownText(String dropdownInitText) throws InterruptedException {
        log.info("Looking for list items within div with text \"" + dropdownInitText + "\"");
        List<WebElement> elements = waitForOneOf(20, getByByExactText("div", dropdownInitText)).findElements(By.cssSelector("li"));
        log.info("Found " + elements.size() + " dropdown options");
        return elements;
    }

    public static void navigateAwayFromInputElement(String elementType, String idOrLabel) throws InterruptedException {
        log.info("Looking for matching element for type " + elementType + " and id/label " + idOrLabel + " and sending TAB key");
        WebElement inputElement = waitForOneOf(20, getBysForInputAreaByLabelOrId(elementType, idOrLabel));
        log.info("Found: " + inputElement);
        inputElement.sendKeys(Keys.TAB);
    }

    public static By[] getBysForInputAreaByLabelOrId(String elementType, String labelOrId) {
        log.info("Retrieving Bys for input " + elementType + " by " + labelOrId);
        By inputByFor = By.xpath("//" + elementType + "[@id=//label[translate(normalize-space(text()), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"" + labelOrId + "\"]/@for]");
        By inputById = null;
        if (!labelOrId.contains(" ")) inputById = By.cssSelector(elementType + "#" + labelOrId);
        if (inputById != null) return new By[]{inputByFor, inputById};
        else return new By[]{inputByFor};
    }

    public static String getElementValue(String element) {
        log.info("Retrieving the value of " + element);
        String value = null;
        switch (element.toLowerCase()) {
            case "order number":
                Matcher m = Pattern.compile("\\d+").matcher(Driver.findElement(By.xpath("//h2[@id='orderplaced-js']|//h2[@class='checkout-confirm-header']")).getText());
                if (m.find()) value = m.group();
                break;
            default:
                log.warn(element + " not defined.");
                throw new IllegalArgumentException(element + " is not a recognised element");
        }
        return value;
    }
    public static void pressEnterOnInputElement(WebElement element) {
        log.info("Pressing enter on element => " + element);
        element.sendKeys(Keys.ENTER);
    }

    public static void assertElementContainsAllTextIn(WebElement element, DataTable dataToBeAsserted) {
        assertElementContainsAllTextIn(element, dataToBeAsserted.asList());
    }

    public static void assertElementContainsAllTextIn(WebElement element, List<String> dataToBeAsserted) {
        waitForElementToBeVisible(element);
        String elementText = element.getText();
        log.info("web element text is => " + elementText);
        for (String dataToAssert : dataToBeAsserted) {
            log.info("Going to assert WebElement text contains =>" + dataToAssert);
            assertThat(elementText).contains(dataToAssert);
        }
    }

    public static void assertLinkVisibility(String linkText, boolean isVisible) {
        By linkXpath = By.xpath(String.format("//a[text()='%s']", linkText));
        if(isVisible) {
            assertWithMessage(String.format("Expected link with text \"%s\" to be displayed but was not displayed", linkText)).that(checkIfElementDisplayed(linkXpath)).isTrue();
        }
        else {
            assertWithMessage(String.format("Expected link with text \"%s\" to not be displayed but was displayed", linkText)).that(checkIfElementDisplayed(linkXpath)).isFalse();
        }
    }


    public static void assertTextInModal(String text,String itemNumber) throws InterruptedException {
        log.info("Going to assert =>"+text+" in the popup for item number =>"+itemNumber);
        By itemTextInModal = By.xpath("//div[@class='modal-content']//h4[@data-variant-code='" + itemNumber + "']/ancestor::div[contains(@class,'row') and ancestor::div[contains(@class,'sku-wrapper')]]");
        assertWithMessage(String.format("Expected text \"%s\" to be displayed but was not displayed", text)).that(checkIfElementDisplayed(waitForOneOf(5, itemTextInModal))).isTrue();
    }


    public static void appendSemiColonToStringBuilder(StringBuilder stringBuilder) {
        stringBuilder.append(";");
    }

    public static boolean messageWithBoldTextIsVisible(String text) {
        log.info("Find text \"" + text + "\" in element b");
        WebElement boldText = Driver.findElement(By.xpath("//b[contains(text(),'" + text + "')]"));
        return boldText.isDisplayed();
    }


    public static String getOrderQuantityInterval(WebElement element) {
        return element.getAttribute("orderquantityinterval");
    }

    public static String getMinimumOrderQuantity(WebElement element) {
        return element.getAttribute("minorderquantity");
    }


    public static void checkImageIsDisplayedByAltTextOrTitle(String altTextOrTitle) {
        assertWithMessage(String.format("Expected image with alt text or title \"%s\" to be displayed but was not displayed", altTextOrTitle))
                .that(BasePage.checkIfElementDisplayed(By.xpath(String.format("//img[@title=\"%1$s\"]|//img[@alt=\"%1$s\"]", altTextOrTitle)))).isTrue();
    }

    /**
     * Clicks on button or a with text @param textOnUI a specified @param numOfTimes
     */
    public static void clickButtonOrLinkMultipleTimes(String textOnUI, int numOfTimes) throws Exception {
        log.info(String.format("Clicking the link/button %s a total of %s number of times", textOnUI, numOfTimes));
        for(int i=0; i<numOfTimes; i++) {
            WebElement uiTextButtonLink = waitForVisibleElement(By.xpath(String.format("//a[text()='%1$s']|//button[text()='%1$s']", textOnUI)), true);
            waitAndClickWithRetry(uiTextButtonLink);
            BasePage.waitForPageToLoad();
        }
        log.info(String.format("Successfully clicked the link/button %s a total of %s number of times", textOnUI, numOfTimes));
    }

    /**
     * Assert the input expected is equal to actual
     * @param inputText this can be today/tomorrow date
     * convert to switch/case if more than two dynamic options (e.g. next week)
     * Also may need to extend to include param whether to include weekends or bank holidays if specified for deliveries
     */
    public static void assertInputWithLabelIsEqualTo(String inputText, String labelText) throws InterruptedException {
        String inputTextToAssert = checkAndReturnDateFormatIfDate(inputText, "dd/MM/yyyy");
        log.info(String.format("Setting date to assert to be \"%s\"", inputTextToAssert));
        log.info(String.format("Getting WebElement input with an associated %s label", labelText));
        String actualText = getInputFieldByLabelText(labelText).getAttribute("value");
        assertWithMessage(String.format("Expected input \"%s\" does not equal actual input \"%s\"",inputText, actualText)).that(actualText.equals(inputTextToAssert)).isTrue();
    }

    public static String checkAndReturnDateFormatIfDate(String text, String dateFormat) {
        switch (text) {
            case "today":
                log.info("Found expected value to be today, setting actual date");
                return getTodayDate(dateFormat);
            case "tomorrow":
                log.info("Found expected value to be tomorrow, setting actual date");
                return getDaysFromToday(1, dateFormat);
            case "yesterday":
                return getDaysFromToday(-1, dateFormat);
            default:
                log.info(String.format("Custom date value not specified, keeping value as %s", text));
                return text;
        }
    }

    public static WebElement getInputFieldByLabelText(String labelText) throws InterruptedException {
        return waitForOneOf(10, By.xpath(String.format("//label[text()='%1$s']/following-sibling::input|//label[text()='%1$s']/parent::div//input", labelText)));
    }

    public static void clickUntilElementIsInvisible(WebElement element) throws Exception {
        log.info("Going to click element till invisible =>" + element);
        byte i = 0;
        byte maxRetries = 3;
        removeImplicitWait();
        while (i < maxRetries) {
            try {
                BasePage.waitForElementToBeInvisible(element);
            } catch (TimeoutException e) {
                log.info("Timeout exception occurred as element still visible, continuing....");
            }
            if (!checkIfElementDisplayed(element)) {
                log.info("element is not visible, returning without click retry");
                applyDefaultImplicitWait();
                return;
            }
            log.info("element is visible , clicking attempt =>" + i);
            waitAndClickWithRetry(element);
            i++;
        }
        throw new Exception("element is visible after max retry click attempts =>" + maxRetries);
    }

    public static void assertTextOnPage(String text1, String text2) {
        log.info("Extracting body text and asserting contains " + text1 + " or " + text2);
        String pageText = Driver.findElement(By.tagName("body")).getText().toLowerCase();
        boolean hasText1 = pageText.contains(text1.toLowerCase());
        log.info("Found text1 (" + text1 + ") was " + (hasText1 ? "" : "not ") + "displayed");
        boolean hasText2 = pageText.contains(text2.toLowerCase());
        log.info("Found text2 (" + text2 + ") was " + (hasText2 ? "" : "not ") + "displayed");
        String failureMessage;
        if (hasText1 || hasText2) {
            failureMessage = "Expected page to contain either \"" + text1 + "\" or \"" + text2 + "\", but contained both";
        } else {
            failureMessage = "Expected page to contain either \"" + text1 + "\" or \"" + text2 + "\", but contained neither";
        }
        assertWithMessage(failureMessage).that(hasText1 ^ hasText2).isTrue();
    }

    public static void clickToCloseModal(String modalTitle) throws Exception {
        WebElement modalTitleCloseButton = Driver.findElement(By.xpath("//h4[contains(text(),'" + modalTitle + "')]//preceding-sibling::button[contains(@class,'close')]"));
        log.info("Closing " + modalTitle + " modal");
        waitAndClickWithRetry(modalTitleCloseButton);
    }
}
