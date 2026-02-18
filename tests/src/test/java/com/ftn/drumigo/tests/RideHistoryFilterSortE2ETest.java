package com.ftn.drumigo.tests;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ftn.drumigo.tests.base.BaseE2ETest;
import com.ftn.drumigo.tests.pages.AdminRideHistoryPage;
import com.ftn.drumigo.tests.pages.LoginPage;

class RideHistoryFilterSortE2ETest extends BaseE2ETest {

    @Test
    void shouldFilterRideHistoryByDateRange_happyPath() {
        AdminRideHistoryPage historyPage = loginAndOpenAdminHistory();

        int initialCount = historyPage.getDisplayedResultsCount();
        Assertions.assertTrue(initialCount > 0,
            "Expected seeded admin ride history to contain at least one ride before filtering.");

        List<LocalDate> initialDates = historyPage.getDisplayedRideDates();
        Assertions.assertFalse(initialDates.isEmpty(),
            "Ride history table is empty before filter; cannot verify date filtering.");

        LocalDate latestDate = initialDates.stream().max(Comparator.naturalOrder()).orElseThrow();
        LocalDate from = latestDate.minusDays(2);
        LocalDate to = latestDate;

        historyPage.applyDateRange(from, to);

        int filteredCount = historyPage.getDisplayedResultsCount();
        List<LocalDate> filteredDates = historyPage.getDisplayedRideDates();

        Assertions.assertTrue(filteredCount > 0,
            "Filtering by recent range should keep at least one ride.");
        Assertions.assertTrue(filteredCount <= initialCount,
            "Filtered results should not exceed initial results.");
        Assertions.assertTrue(filteredDates.stream().allMatch(d -> !d.isBefore(from) && !d.isAfter(to)),
            "All displayed rides must be within selected [from, to] date range.");
    }

    @Test
    void shouldSortRideHistoryByDateAscendingThenDescending_happyPath() {
        AdminRideHistoryPage historyPage = loginAndOpenAdminHistory();

        historyPage.clickDateSortHeader(); // asc
        List<LocalDate> ascendingDates = historyPage.getDisplayedRideDates();
        assertSortedAscending(ascendingDates);

        historyPage.clickDateSortHeader(); // desc
        List<LocalDate> descendingDates = historyPage.getDisplayedRideDates();
        assertSortedDescending(descendingDates);
    }

    @Test
    void shouldHandleInvalidDateRangeFromAfterTo_exceptionCase() {
        AdminRideHistoryPage historyPage = loginAndOpenAdminHistory();

        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().minusDays(30);
        historyPage.applyDateRange(from, to);

        int resultsCount = historyPage.getDisplayedResultsCount();
        Assertions.assertTrue(historyPage.isEmptyStateVisible() || resultsCount == 0,
            "Invalid date range should produce no results and/or empty state.");
        Assertions.assertTrue(driver.getCurrentUrl().contains("/admin/ride-history"),
            "Page should remain stable after invalid filter input.");
    }

    @Test
    void shouldClearFilterAndRestoreResults_exceptionRecovery() {
        AdminRideHistoryPage historyPage = loginAndOpenAdminHistory();

        int baselineCount = historyPage.getDisplayedResultsCount();
        Assertions.assertTrue(baselineCount > 0,
            "Expected seeded admin ride history to contain data before recovery scenario.");

        historyPage.applyDateRange(LocalDate.now(), LocalDate.now().minusDays(30));
        int afterInvalidFilterCount = historyPage.getDisplayedResultsCount();

        historyPage.clearFilters();
        int restoredCount = historyPage.getDisplayedResultsCount();

        Assertions.assertTrue(restoredCount > afterInvalidFilterCount,
            "Clearing filters should restore more results than invalid range filter.");
    }

    private AdminRideHistoryPage loginAndOpenAdminHistory() {
        LoginPage loginPage = new LoginPage(driver, wait);
        loginPage.open(frontendUrl);
        loginPage.loginExpectingRedirect(adminEmail, adminPassword, "/admin/ride-history");

        AdminRideHistoryPage historyPage = new AdminRideHistoryPage(driver, wait);
        historyPage.waitUntilLoaded();
        historyPage.waitForLoadingToFinish();
        return historyPage;
    }

    private void assertSortedAscending(List<LocalDate> dates) {
        for (int i = 1; i < dates.size(); i++) {
            Assertions.assertFalse(dates.get(i).isBefore(dates.get(i - 1)),
                "Dates are not sorted ascending at index " + i + ": " + dates);
        }
    }

    private void assertSortedDescending(List<LocalDate> dates) {
        for (int i = 1; i < dates.size(); i++) {
            Assertions.assertFalse(dates.get(i).isAfter(dates.get(i - 1)),
                "Dates are not sorted descending at index " + i + ": " + dates);
        }
    }
}
