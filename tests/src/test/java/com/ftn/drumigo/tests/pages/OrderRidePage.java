package com.ftn.drumigo.tests.pages;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class OrderRidePage {
    private static final By STEP_TITLE = By.xpath("//h2[contains(@class,'step-title') and contains(., 'Where are you going?')]");
    private static final By PICKUP_INPUT = By.id("pickup");
    private static final By DESTINATION_INPUT = By.id("destination");
    /** Container shown when user has favorite routes (card-based UI, not a select). */
    private static final By FAVORITE_ROUTES_PICKER = By.cssSelector(".favorite-routes-picker");
    /** Favorite route cards only (excludes "Enter address manually" card). */
    private static final By FAVORITE_ROUTE_CARD_NOT_MANUAL = By.cssSelector(".favorite-route-card:not(.favorite-route-card--manual)");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public OrderRidePage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void waitUntilLoaded() {
        wait.until(ExpectedConditions.or(
            ExpectedConditions.visibilityOfElementLocated(STEP_TITLE),
            ExpectedConditions.visibilityOfElementLocated(PICKUP_INPUT)
        ));
    }

    /**
     * Wait until the favorite routes picker is visible and has loaded (optional, call before asserting on favorites).
     */
    public void waitForFavoritesToLoad() {
        wait.until(d -> {
            List<WebElement> pickers = d.findElements(FAVORITE_ROUTES_PICKER);
            if (pickers.isEmpty() || !pickers.get(0).isDisplayed()) {
                return false;
            }
            return !d.findElements(FAVORITE_ROUTE_CARD_NOT_MANUAL).isEmpty();
        });
    }

    /**
     * True when the favorite routes picker (card-based UI) is visible and has at least one favorite.
     */
    public boolean isFavoriteSelectVisible() {
        List<WebElement> pickers = driver.findElements(FAVORITE_ROUTES_PICKER);
        if (pickers.isEmpty() || !pickers.get(0).isDisplayed()) {
            return false;
        }
        List<WebElement> favoriteCards = driver.findElements(FAVORITE_ROUTE_CARD_NOT_MANUAL);
        return !favoriteCards.isEmpty();
    }

    /**
     * Number of favorite route cards (excluding "Enter address manually").
     */
    public int getFavoriteOptionCount() {
        if (!isFavoriteSelectVisible()) {
            return 0;
        }
        return driver.findElements(FAVORITE_ROUTE_CARD_NOT_MANUAL).size();
    }

    /**
     * Select favorite by 1-based index. Index 1 = first favorite, 2 = second, etc.
     */
    public void selectFavoriteByIndex(int index) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(FAVORITE_ROUTES_PICKER));
        List<WebElement> cards = driver.findElements(FAVORITE_ROUTE_CARD_NOT_MANUAL);
        Assertions.assertTrue(index >= 1 && index <= cards.size(),
            "Favorite index " + index + " out of range; found " + cards.size() + " favorite(s).");
        wait.until(ExpectedConditions.elementToBeClickable(cards.get(index - 1))).click();
    }

    /**
     * Wait until destination field has been filled (e.g. after selecting a favorite).
     */
    public void waitForPreFill() {
        wait.until(d -> {
            WebElement input = d.findElement(DESTINATION_INPUT);
            String value = input.getAttribute("value");
            return value != null && !value.isBlank();
        });
    }

    public String getPickupValue() {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(PICKUP_INPUT));
        return input.getAttribute("value");
    }

    public String getDestinationValue() {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(DESTINATION_INPUT));
        return input.getAttribute("value");
    }
}
