package com.ui.util;

import com.ui.pages.CookieFooterModal;
import org.apache.http.Header;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.ElementClickInterceptedException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.ui.pages.BasePage.*;

public class CookieHelper {

	private final static Pattern PATTERN = Pattern.compile("JSESSIONID=[a-zA-Z0-9_.-]*");
	private static CookieHelper cookieParser = new CookieHelper();
	private static Logger log = LogManager.getLogger(CookieHelper.class);


	public static CookieHelper getInstance() {
		return cookieParser;
	}

	private CookieHelper() {
	}

	public String getSpecialCookie(final Header[] headers) {
		if(headers.length > 0) {
			for (final Header header : headers) {
				if ("Set-Cookie".equals(header.getName())) {
					final Matcher matcher = PATTERN.matcher(header.getValue());
					if (matcher.find()) {
						final String jsessionId = matcher.group(0);
						if (jsessionId != null && jsessionId.split("=").length > 1) {
							return jsessionId.split("=")[1];
						}
					}
				}
			}
		}
		return null;
	}
	public static void dismissCookieFooter() {

		removeImplicitWait();
		try {
			Driver.findElement(By.className("cc-dismiss")).click();
			log.info("Dismissed the cookie footer");
		} catch (ElementClickInterceptedException e) {
			log.warn("Click to dismiss cookie footer was intercepted: " + e);
			log.info("Scrolling to top and retrying...");
			scrollToTop(Driver);
			Driver.findElement(By.className("cc-dismiss")).click();
			log.info("Dismissed the cookie footer");
		} catch(Exception e) {
			log.info("Cookie footer not present - continuing without error");
			return;
		}
		applyDefaultImplicitWait();
		//Wait until invisible class name is applied to cookie footer
		waitUntilElementExist(CookieFooterModal.invisibleCookieFooter);
	}
	public static void deleteAllCookies() {
		log.info("Checking if any cookies exist");
		if(Driver.manage().getCookies().isEmpty()){
			log.info("There are no cookies to be deleted");
		}
		else {
			log.info("At least one cookie was identified. Deleting all cookies...");
			Driver.manage().deleteAllCookies();
			log.info("Cookies have been deleted");
		}
	}
	private static Cookie getCookieConsentStatusCookie() {
		return Driver.manage().getCookieNamed("cookieconsent_status");
	}
	public static void assertCookieConsentStatusDismiss() {
		log.info("Checking that the cookie \"cookieconsent_status\" has the value \"dismiss\"");
		assertWithMessage("\"cookieconsent_status\" did not have the value \"dismiss\" or was not found")
				.that(getCookieConsentStatusCookie().getValue()).isEqualTo("dismiss");
		log.info("Cookie \"cookieconsent_status\" has the value \"dismiss\"");
	}
	public static void checkDismissCookieNotPresent() {
		if(getCookieConsentStatusCookie() != null) {
			log.warn("Cookie \"cookieconsent_status\" is present. Cookie has been dismissed when not expected.");
		}
		else {
			log.info("Cookie \"cookieconsent_status\" is not present as expected.");
		}
	}
}
