package com.ftn.drumigo.tests.base;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BaseE2ETest {
    protected WebDriver driver;
    protected WebDriverWait wait;

    protected String frontendUrl;
    protected String adminEmail;
    protected String adminPassword;

    @BeforeEach
    void setUpDriver() {
        frontendUrl = getConfig("e2e.frontend.url", "E2E_FRONTEND_URL", "http://localhost:4200");
        adminEmail = getConfig("e2e.admin.email", "E2E_ADMIN_EMAIL", "stefan.nikolic@drumigo.com");
        adminPassword = getConfig("e2e.admin.password", "E2E_ADMIN_PASSWORD", "Sifra123");
        boolean headless = Boolean.parseBoolean(getConfig("e2e.headless", "E2E_HEADLESS", "true"));

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        if (headless) {
            options.addArguments("--headless=new");
        }

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @AfterEach
    void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    private String getConfig(String propertyKey, String envKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return defaultValue;
    }
}
