package com.ftn.drumigo.tests.pages;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
    private static final By DETAIL_PANEL = By.cssSelector(".detail-panel");
    private static final By DETAIL_CLOSE_BUTTON = By.cssSelector(".btn-close-detail");
    private static final By ROUTE_FROM = By.cssSelector(".route-from");
    private static final By ROUTE_TO = By.cssSelector(".route-to");
    private static final By RATE_BUTTON = By.cssSelector(".btn-rate");
    private static final By RATING_SUBMITTED = By.cssSelector(".rating-submitted");
    private static final By RATING_EXPIRED = By.cssSelector(".rating-expired");
    private static final By RATING_LOADING = By.cssSelector(".rating-loading");
    private static final By RATING_VALUES = By.cssSelector(".rating-value");
    private static final By RATING_MODAL = By.cssSelector("app-rating-modal .modal");
    private static final By RATING_MODAL_CLOSE = By.cssSelector("app-rating-modal .modal__close");
    private static final By COMMENT_INPUT = By.cssSelector("app-rating-modal textarea#comment");
    private static final By SUBMIT_RATING_BUTTON = By.cssSelector("app-rating-modal button[type='submit']");
    private static final By DRIVER_RATING_ERROR = By.cssSelector(
        "app-rating-modal .rating-form__field:nth-of-type(1) .rating-form__error"
    );
    private static final By VEHICLE_RATING_ERROR = By.cssSelector(
        "app-rating-modal .rating-form__field:nth-of-type(2) .rating-form__error"
    );

    private final WebDriver driver;
    private final WebDriverWait wait;

    public PassengerRideHistoryPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void open(String frontendUrl) {
        driver.get(frontendUrl + "/ride-history");
        waitUntilLoaded();
        waitForLoadingToFinish();
    }

    public void waitUntilLoaded() {
        WebElement title = wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_TITLE));
        Assertions.assertTrue(title.getText().toLowerCase(Locale.ROOT).contains("my rides"),
            "Passenger ride history page title not detected. Found: " + title.getText());
    }

    public void waitForTableOrEmpty() {
        waitForLoadingToFinish();
    }

    public void waitForLoadingToFinish() {
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

    public void selectRideByRoute(String fromContains, String toContains) {
        waitForLoadingToFinish();

        String fromNeedle = fromContains.toLowerCase(Locale.ROOT);
        String toNeedle = toContains.toLowerCase(Locale.ROOT);

        boolean clicked = wait.until(d -> {
            try {
                List<WebElement> rows = d.findElements(TABLE_ROWS);
                for (WebElement row : rows) {
                    String fromText = row.findElement(ROUTE_FROM).getText().toLowerCase(Locale.ROOT);
                    String toText = row.findElement(ROUTE_TO).getText().toLowerCase(Locale.ROOT);

                    if (fromText.contains(fromNeedle) && toText.contains(toNeedle)) {
                        ((JavascriptExecutor) d).executeScript(
                            "arguments[0].scrollIntoView({block:'center'});",
                            row
                        );
                        row.click();
                        return true;
                    }
                }
                return false;
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        if (!clicked) {
            Assertions.fail("Could not find ride row for route: " + fromContains + " -> " + toContains);
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(DETAIL_PANEL));
    }

    public void closeRideDetailsIfOpen() {
        List<WebElement> panels = driver.findElements(DETAIL_PANEL);
        if (panels.isEmpty() || !panels.get(0).isDisplayed()) {
            return;
        }

        wait.until(ExpectedConditions.elementToBeClickable(DETAIL_CLOSE_BUTTON)).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(DETAIL_PANEL));
    }

    public void waitForRatingStatusResolved() {
        wait.until(d -> {
            try {
                List<WebElement> loaders = d.findElements(RATING_LOADING);
                if (loaders.isEmpty()) {
                    return true;
                }
                return loaders.stream().noneMatch(WebElement::isDisplayed);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        wait.until(d ->
            isDisplayed(d, RATE_BUTTON) || isDisplayed(d, RATING_SUBMITTED) || isDisplayed(d, RATING_EXPIRED)
        );
    }

    public boolean isRateButtonVisible() {
        return isDisplayed(driver, RATE_BUTTON);
    }

    public boolean isRatingSubmittedVisible() {
        return isDisplayed(driver, RATING_SUBMITTED);
    }

    public boolean isRatingExpiredVisible() {
        return isDisplayed(driver, RATING_EXPIRED);
    }

    public void openRatingModal() {
        wait.until(ExpectedConditions.elementToBeClickable(RATE_BUTTON)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(RATING_MODAL));
    }

    public boolean isRatingModalVisible() {
        return isDisplayed(driver, RATING_MODAL);
    }

    public void setDriverRating(int stars) {
        setRating(1, stars);
    }

    public void setVehicleRating(int stars) {
        setRating(2, stars);
    }

    public void setComment(String comment) {
        WebElement textarea = wait.until(ExpectedConditions.visibilityOfElementLocated(COMMENT_INPUT));
        textarea.clear();
        textarea.sendKeys(comment);
    }

    public void submitRating() {
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_RATING_BUTTON)).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(RATING_MODAL));
    }

    public void submitRatingExpectingValidationError() {
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_RATING_BUTTON)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(RATING_MODAL));
    }

    public void closeRatingModal() {
        wait.until(ExpectedConditions.elementToBeClickable(RATING_MODAL_CLOSE)).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(RATING_MODAL));
    }

    public boolean isDriverRatingValidationVisible() {
        return isDisplayed(driver, DRIVER_RATING_ERROR);
    }

    public boolean isVehicleRatingValidationVisible() {
        return isDisplayed(driver, VEHICLE_RATING_ERROR);
    }

    public List<String> getDisplayedRatingValues() {
        return wait.until(d -> {
            List<WebElement> values = d.findElements(RATING_VALUES);
            if (values.isEmpty()) {
                return null;
            }
            return values.stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .toList();
        });
    }

    private void setRating(int fieldIndex, int stars) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Rating stars must be in range 1-5.");
        }

        String ariaLabel = stars == 1 ? "1 star" : stars + " stars";
        By field = By.cssSelector("app-rating-modal .rating-form__field:nth-of-type(" + fieldIndex + ")");
        By starButton = By.cssSelector("button.star-rating__star[aria-label='" + ariaLabel + "']");

        boolean clicked = wait.until(d -> {
            try {
                WebElement formField = d.findElement(field);
                WebElement star = formField.findElement(starButton);
                if (star.isDisplayed() && star.isEnabled()) {
                    star.click();
                    return true;
                }
                return false;
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        if (!clicked) {
            Assertions.fail("Could not click rating star " + stars + " in field index " + fieldIndex + ".");
        }
    }

    private boolean isDisplayed(WebDriver contextDriver, By locator) {
        try {
            List<WebElement> elements = contextDriver.findElements(locator);
            return elements.stream().anyMatch(WebElement::isDisplayed);
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }
}
