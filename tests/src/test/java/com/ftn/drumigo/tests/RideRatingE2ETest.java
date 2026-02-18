package com.ftn.drumigo.tests;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ftn.drumigo.tests.base.BaseE2ETest;
import com.ftn.drumigo.tests.pages.LoginPage;
import com.ftn.drumigo.tests.pages.PassengerRideHistoryPage;

class RideRatingE2ETest extends BaseE2ETest {

    private static final String DEFAULT_RATING_PASSENGER_EMAIL = "rating.passenger@test.local";
    private static final String DEFAULT_RATING_PASSENGER_PASSWORD = "Password12345";

    @Test
    void shouldSubmitDriverAndVehicleRatings_happyPath() {
        PassengerRideHistoryPage historyPage = loginAndOpenPassengerHistory();

        historyPage.selectRideByRoute("Podbara", "Grbavica");
        historyPage.waitForRatingStatusResolved();
        Assertions.assertTrue(historyPage.isRateButtonVisible(),
            "Expected selected ride to be rateable before submission.");

        historyPage.openRatingModal();
        historyPage.setDriverRating(5);
        historyPage.setVehicleRating(4);
        historyPage.setComment("Automated Student 2 E2E rating submission.");
        historyPage.submitRating();
        historyPage.waitForRatingStatusResolved();

        Assertions.assertFalse(historyPage.isRateButtonVisible(),
            "Rate button should disappear after successful submission.");
        Assertions.assertTrue(historyPage.isRatingSubmittedVisible(),
            "Submitted state should be shown after successful rating.");

        List<String> ratingValues = historyPage.getDisplayedRatingValues();
        Assertions.assertTrue(ratingValues.contains("5/5"),
            "Expected submitted driver rating 5/5 in details panel, got: " + ratingValues);
        Assertions.assertTrue(ratingValues.contains("4/5"),
            "Expected submitted vehicle rating 4/5 in details panel, got: " + ratingValues);
    }

    @Test
    void shouldNotAllowRatingWhenRideAlreadyReviewed_exceptionCase() {
        PassengerRideHistoryPage historyPage = loginAndOpenPassengerHistory();

        historyPage.selectRideByRoute("Bulevar Oslobodjenja", "Trg Slobode");
        historyPage.waitForRatingStatusResolved();

        Assertions.assertTrue(historyPage.isRatingSubmittedVisible(),
            "Already reviewed ride should show submitted-rating state.");
        Assertions.assertFalse(historyPage.isRateButtonVisible(),
            "Already reviewed ride must not display rate button.");
    }

    @Test
    void shouldNotAllowRatingWhenDeadlinePassed_exceptionCase() {
        PassengerRideHistoryPage historyPage = loginAndOpenPassengerHistory();

        historyPage.selectRideByRoute("Detelinara", "Telep");
        historyPage.waitForRatingStatusResolved();

        Assertions.assertTrue(historyPage.isRatingExpiredVisible(),
            "Expired ride should show rating deadline passed state.");
        Assertions.assertFalse(historyPage.isRateButtonVisible(),
            "Expired ride must not display rate button.");
    }

    @Test
    void shouldRequireBothRatingsBeforeSubmit_validationCase() {
        PassengerRideHistoryPage historyPage = loginAndOpenPassengerHistory();

        historyPage.selectRideByRoute("Petrovaradin", "Liman");
        historyPage.waitForRatingStatusResolved();
        Assertions.assertTrue(historyPage.isRateButtonVisible(),
            "Selected ride should be rateable for validation scenario.");

        historyPage.openRatingModal();
        historyPage.submitRatingExpectingValidationError();

        Assertions.assertTrue(historyPage.isRatingModalVisible(),
            "Rating modal should remain open when required ratings are missing.");
        Assertions.assertTrue(historyPage.isDriverRatingValidationVisible(),
            "Driver rating validation should be shown when missing.");
        Assertions.assertTrue(historyPage.isVehicleRatingValidationVisible(),
            "Vehicle rating validation should be shown when missing.");

        historyPage.closeRatingModal();
        Assertions.assertTrue(historyPage.isRateButtonVisible(),
            "Ride should remain rateable after closing invalid submission attempt.");
    }

    private PassengerRideHistoryPage loginAndOpenPassengerHistory() {
        LoginPage loginPage = new LoginPage(driver, wait);
        loginPage.open(frontendUrl);
        loginPage.loginExpectingRedirect(
            resolveConfig(
                new String[] {"e2e.rating.passenger.email", "e2e.passenger.email"},
                new String[] {"E2E_RATING_PASSENGER_EMAIL", "E2E_PASSENGER_EMAIL"},
                DEFAULT_RATING_PASSENGER_EMAIL
            ),
            resolveConfig(
                new String[] {"e2e.rating.passenger.password", "e2e.passenger.password"},
                new String[] {"E2E_RATING_PASSENGER_PASSWORD", "E2E_PASSENGER_PASSWORD"},
                DEFAULT_RATING_PASSENGER_PASSWORD
            ),
            "/order-ride"
        );

        PassengerRideHistoryPage historyPage = new PassengerRideHistoryPage(driver, wait);
        historyPage.open(frontendUrl);
        return historyPage;
    }

    private String resolveConfig(String[] propertyKeys, String[] envKeys, String defaultValue) {
        for (String propertyKey : propertyKeys) {
            String propertyValue = System.getProperty(propertyKey);
            if (propertyValue != null && !propertyValue.isBlank()) {
                return propertyValue;
            }
        }
        for (String envKey : envKeys) {
            String envValue = System.getenv(envKey);
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
        }
        return defaultValue;
    }
}
