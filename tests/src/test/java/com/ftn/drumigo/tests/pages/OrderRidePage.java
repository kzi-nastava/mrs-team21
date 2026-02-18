package com.ftn.drumigo.tests.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class OrderRidePage {
    private static final By STEP_TITLE = By.xpath("//h2[contains(@class,'step-title') and contains(., 'Where are you going?')]");
    private static final By PICKUP_INPUT = By.id("pickup");
    private static final By FAVORITE_SELECT = By.id("favorite-select");
    private static final By DESTINATION_INPUT = By.id("destination");

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

    public boolean isFavoriteSelectVisible() {
        List<WebElement> elements = driver.findElements(FAVORITE_SELECT);
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }

    /**
     * Number of option elements in the favorite select that represent actual favorites
     * (i.e. excluding the "None" option with value "").
     */
    public int getFavoriteOptionCount() {
        if (!isFavoriteSelectVisible()) {
            return 0;
        }
        WebElement selectEl = driver.findElement(FAVORITE_SELECT);
        Select select = new Select(selectEl);
        List<WebElement> options = select.getOptions();
        int count = 0;
        for (WebElement opt : options) {
            String value = opt.getAttribute("value");
            if (value != null && !value.isBlank()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Select by visible index in the dropdown. Index 0 is typically "None", index 1 is first favorite.
     */
    public void selectFavoriteByIndex(int index) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(FAVORITE_SELECT));
        WebElement selectEl = driver.findElement(FAVORITE_SELECT);
        Select select = new Select(selectEl);
        select.selectByIndex(index);
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
