package com.ftn.drumigo.tests.pages;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AdminRideHistoryPage {
    private static final By PAGE_TITLE = By.cssSelector(".page-title");
    private static final By LOADING_BANNER = By.cssSelector(".loading-banner");
    private static final By DATE_FILTER_INPUTS =
        By.cssSelector("app-ride-history-filters input.filter-input[type='date']");
    private static final By CLEAR_FILTER_BUTTON = By.cssSelector("app-ride-history-filters .btn-filter");
    private static final By RESULTS_COUNT = By.cssSelector("app-ride-history-filters .results-count");
    private static final By TABLE_ROWS = By.cssSelector("app-ride-history-table tbody tr");
    private static final By EMPTY_STATE = By.cssSelector("app-ride-history-table .empty-state");
    private static final By DATE_SORT_HEADER =
        By.xpath("//th[contains(@class,'sortable-header') and contains(., 'Date & Time')]");
    private static final By DATE_VALUE_IN_ROW = By.cssSelector(".date-value");

    private static final Pattern RESULTS_PATTERN = Pattern.compile("(\\d+)");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final DateTimeFormatter tableDateFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    public AdminRideHistoryPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void waitUntilLoaded() {
        WebElement title = wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_TITLE));
        Assertions.assertTrue(title.getText().toLowerCase(Locale.ROOT).contains("ride history"),
            "Admin ride history page title not detected. Found: " + title.getText());
    }

    public void waitForLoadingToFinish() {
        wait.until(d -> {
            try {
                List<WebElement> banners = d.findElements(LOADING_BANNER);
                if (banners.isEmpty()) {
                    return true;
                }
                return banners.stream().noneMatch(WebElement::isDisplayed);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        wait.until(d -> {
            try {
                return !d.findElements(TABLE_ROWS).isEmpty() || !d.findElements(EMPTY_STATE).isEmpty();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    public int getDisplayedResultsCount() {
        String text = wait.until(ExpectedConditions.visibilityOfElementLocated(RESULTS_COUNT)).getText();
        Matcher matcher = RESULTS_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw new AssertionError("Could not parse results count from text: " + text);
        }
        return Integer.parseInt(matcher.group(1));
    }

    public List<LocalDate> getDisplayedRideDates() {
        return wait.until(d -> {
            try {
                List<WebElement> rows = d.findElements(TABLE_ROWS);
                return rows.stream()
                    .map(row -> row.findElement(DATE_VALUE_IN_ROW).getText().trim())
                    .map(this::parseTableDate)
                    .toList();
            } catch (StaleElementReferenceException e) {
                return null;
            }
        });
    }

    public boolean isEmptyStateVisible() {
        try {
            List<WebElement> emptyStateElements = driver.findElements(EMPTY_STATE);
            return !emptyStateElements.isEmpty() && emptyStateElements.get(0).isDisplayed();
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    public void applyDateRange(LocalDate from, LocalDate to) {
        setDateInput(0, from);
        setDateInput(1, to);
        waitForLoadingToFinish();
    }

    public void clearFilters() {
        wait.until(ExpectedConditions.elementToBeClickable(CLEAR_FILTER_BUTTON)).click();
        waitForLoadingToFinish();
    }

    public void clickDateSortHeader() {
        wait.until(ExpectedConditions.elementToBeClickable(DATE_SORT_HEADER)).click();
    }

    private void setDateInput(int index, LocalDate date) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        wait.until(d -> {
            try {
                List<WebElement> inputs = d.findElements(DATE_FILTER_INPUTS);
                if (inputs.size() <= index) {
                    return false;
                }

                WebElement input = inputs.get(index);
                js.executeScript(
                    "arguments[0].value = arguments[1];"
                        + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                        + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    input,
                    date.toString()
                );
                return true;
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    private LocalDate parseTableDate(String value) {
        try {
            return LocalDate.parse(value, tableDateFormatter);
        } catch (DateTimeParseException e) {
            throw new AssertionError("Unexpected date format in table: '" + value + "'", e);
        }
    }
}
