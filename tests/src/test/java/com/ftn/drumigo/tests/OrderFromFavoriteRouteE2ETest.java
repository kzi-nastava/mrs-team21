package com.ftn.drumigo.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ftn.drumigo.tests.base.BaseE2ETest;
import com.ftn.drumigo.tests.pages.LoginPage;
import com.ftn.drumigo.tests.pages.OrderRidePage;
import com.ftn.drumigo.tests.pages.PassengerRideHistoryPage;

/**
 * E2E tests for spec 2.4.3: Ordering from favorite routes.
 * Tests selection and pre-fill only (no order submission); add/remove favorite in history.
 */
class OrderFromFavoriteRouteE2ETest extends BaseE2ETest {

    @Test
    void shouldPreFillPickupAndDestinationWhenSelectingFavorite_selectionOnly() {
        LoginPage loginPage = new LoginPage(driver, wait);
        loginPage.open(frontendUrl);
        loginPage.loginExpectingRedirect(passengerEmail, passengerPassword, "/order-ride");
        driver.get(frontendUrl + "/ride-history");

        PassengerRideHistoryPage historyPage = new PassengerRideHistoryPage(driver, wait);
        historyPage.waitUntilLoaded();
        historyPage.waitForTableOrEmpty();

        Assertions.assertTrue(historyPage.getTableRowsCount() > 0,
            "Passenger must have at least one ride in history (run maintenance reset with seed data).");

        historyPage.clickFavoriteOnRow(0);
        wait.until(d -> historyPage.isRowFavorite(0));

        driver.get(frontendUrl + "/order-ride");
        OrderRidePage orderPage = new OrderRidePage(driver, wait);
        orderPage.waitUntilLoaded();
        orderPage.waitForFavoritesToLoad();

        Assertions.assertTrue(orderPage.isFavoriteSelectVisible(),
            "Favorite picker should be visible when passenger has favorites.");
        Assertions.assertTrue(orderPage.getFavoriteOptionCount() >= 1,
            "At least one favorite option should be available.");

        orderPage.selectFavoriteByIndex(1);
        orderPage.waitForPreFill();

        String pickup = orderPage.getPickupValue();
        String destination = orderPage.getDestinationValue();

        Assertions.assertNotNull(pickup, "Pickup should be pre-filled after selecting favorite.");
        Assertions.assertFalse(pickup.isBlank(), "Pickup should be non-empty after selecting favorite.");
        Assertions.assertNotNull(destination, "Destination should be pre-filled after selecting favorite.");
        Assertions.assertFalse(destination.isBlank(), "Destination should be non-empty after selecting favorite.");
    }

    @Test
    void shouldUpdateFavoritesWhenRemovingFavoriteFromHistory() {
        LoginPage loginPage = new LoginPage(driver, wait);
        loginPage.open(frontendUrl);
        loginPage.loginExpectingRedirect(passengerEmail, passengerPassword, "/order-ride");
        driver.get(frontendUrl + "/ride-history");

        PassengerRideHistoryPage historyPage = new PassengerRideHistoryPage(driver, wait);
        historyPage.waitUntilLoaded();
        historyPage.waitForTableOrEmpty();

        Assertions.assertTrue(historyPage.getTableRowsCount() > 0,
            "Passenger must have at least one ride in history (run maintenance reset with seed data).");

        if (!historyPage.isRowFavorite(0)) {
            historyPage.clickFavoriteOnRow(0);
            wait.until(d -> historyPage.isRowFavorite(0));
        }

        historyPage.clickFavoriteOnRow(0);
        wait.until(d -> !historyPage.isRowFavorite(0));

        driver.get(frontendUrl + "/order-ride");
        OrderRidePage orderPage = new OrderRidePage(driver, wait);
        orderPage.waitUntilLoaded();

        Assertions.assertEquals(0, orderPage.getFavoriteOptionCount(),
            "After removing the only favorite, dropdown should have no favorite options (only 'None').");
    }
}
