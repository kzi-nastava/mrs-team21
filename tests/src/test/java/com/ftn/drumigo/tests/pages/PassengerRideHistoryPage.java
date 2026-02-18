package com.ftn.drumigo.tests.pages;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class PassengerRideHistoryPage {
    private static final By PAGE_TITLE = By.cssSelector(".page-title");
    private static final By LOADING_BANNER = By.cssSelector(".loading-banner");
    private static final By TABLE_ROWS = By.cssSelector("app-ride-history-table tbody tr");
    private static final By EMPTY_STATE = By.cssSelector("app-ride-history-table .empty-state");
    private static final By FAVORITE_BUTTON_IN_ROW = By.cssSelector(".favorite-btn");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public PassengerRideHistoryPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void waitUntilLoaded() {
        WebElement title = wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_TITLE));
        Assertions.assertTrue(title.getText().contains("My Rides"),
            "Passenger ride history page title not detected. Found: " + title.getText());
    }

    public void waitForTableOrEmpty() {
        wait.until(d -> {
            try {
                List<WebElement> banners = d.findElements(LOADING_BANNER);
                if (!banners.isEmpty() && banners.stream().anyMatch(WebElement::isDisplayed)) {
                    return false;
                }
                return !d.findElements(TABLE_ROWS).isEmpty() || !d.findElements(EMPTY_STATE).isEmpty();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    public int getTableRowsCount() {
        List<WebElement> rows = driver.findElements(TABLE_ROWS);
        return rows.size();
    }

    public void clickFavoriteOnRow(int rowIndex) {
        List<WebElement> rows = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(TABLE_ROWS));
        Assertions.assertTrue(rowIndex >= 0 && rowIndex < rows.size(),
            "Row index " + rowIndex + " out of range; table has " + rows.size() + " rows");
        WebElement row = rows.get(rowIndex);
        WebElement favoriteBtn = row.findElement(FAVORITE_BUTTON_IN_ROW);
        wait.until(ExpectedConditions.elementToBeClickable(favoriteBtn)).click();
    }

    public boolean isRowFavorite(int rowIndex) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                List<WebElement> rows = driver.findElements(TABLE_ROWS);
                if (rowIndex < 0 || rowIndex >= rows.size()) {
                    return false;
                }
                WebElement row = rows.get(rowIndex);
                WebElement favoriteBtn = row.findElement(FAVORITE_BUTTON_IN_ROW);
                String classAttr = favoriteBtn.getAttribute("class");
                return classAttr != null && classAttr.contains("is-favorite");
            } catch (StaleElementReferenceException e) {
                if (attempt == 2) {
                    throw e;
                }
                // Table re-rendered after favorite toggle; re-query on next attempt
            }
        }
        return false;
    }
}
