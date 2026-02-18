package com.ftn.drumigo.tests.pages;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage {
    private static final By EMAIL_INPUT = By.id("email");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By SUBMIT_BUTTON = By.cssSelector("form button[type='submit']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void open(String frontendUrl) {
        driver.get(frontendUrl + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT));
    }

    public void loginExpectingRedirect(String email, String password, String expectedUrlFragment) {
        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT));
        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_INPUT));

        emailInput.clear();
        emailInput.sendKeys(email);

        passwordInput.clear();
        passwordInput.sendKeys(password);

        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON)).click();

        WebDriverWait redirectWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        try {
            redirectWait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains(expectedUrlFragment),
                ExpectedConditions.alertIsPresent()
            ));
        } catch (TimeoutException e) {
            try {
                Alert alert = driver.switchTo().alert();
                String alertMessage = alert.getText();
                alert.accept();
                Assertions.fail("Login did not redirect to expected route: " + expectedUrlFragment
                    + ". Alert: " + alertMessage + ". Check credentials (E2E_ADMIN_* or E2E_PASSENGER_*).");
            } catch (Exception ignored) {
                Assertions.fail("Login did not redirect to expected route: " + expectedUrlFragment
                    + ". Current URL: " + driver.getCurrentUrl() + ". Check credentials.");
            }
        }

        // Dismiss alert first if present, so getCurrentUrl() does not throw UnhandledAlertException
        try {
            Alert alert = driver.switchTo().alert();
            String alertMessage = alert.getText();
            alert.accept();
            Assertions.fail("Login failed. Expected URL containing '" + expectedUrlFragment
                + "'. Alert: " + alertMessage + ". Check credentials (E2E_ADMIN_* or E2E_PASSENGER_*).");
        } catch (Exception ignored) {
            // no alert - continue
        }

        if (!driver.getCurrentUrl().contains(expectedUrlFragment)) {
            Assertions.fail("Login failed. Expected URL containing '" + expectedUrlFragment
                + "', current URL: " + driver.getCurrentUrl());
        }
    }
}
