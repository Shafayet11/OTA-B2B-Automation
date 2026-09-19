package com.takeoff.tests;

import com.takeoff.base.AuthenticatedTest;
import com.takeoff.pages.BookingPage;
import com.takeoff.pages.SearchPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;

@Epic("B2B OTA Portal")
@Feature("Booking")
public class BookingTests extends AuthenticatedTest {

    /**
     * Verifies the flow reaches the traveler-details/booking form after
     * selecting a fare. Deliberately does NOT click "Confirm Booking" -
     * doing so submits a real reservation against live fare inventory and
     * charges the agent's account balance (confirmed with the team).
     */
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Description("An agent can search, select a fare, and reach the traveler-details booking form.")
    public void agentCanReachBookingForm() {
        SearchPage searchPage = new SearchPage(page);
        searchPage.searchOneWayFlight("Dhaka", "DAC", "Dubai", "DXB", LocalDate.now().plusMonths(1));
        Assert.assertTrue(searchPage.resultsCount() > 0, "Expected at least one search result");
        searchPage.selectResult(0);

        BookingPage bookingPage = new BookingPage(page);
        Assert.assertTrue(bookingPage.isTravelerDetailsPageDisplayed(),
                "Should reach the traveler details form after selecting a fare");
        Assert.assertTrue(bookingPage.isConfirmBookingButtonVisible(),
                "Confirm Booking button should be visible on the final step");
    }

    /**
     * Completes the full traveler-details form for a Regular fare (the
     * direct "Select" button on a specific booking class) and verifies the
     * booking is ready to confirm. Deliberately stops short of clicking
     * "Confirm Booking" - see the class-level warning on BookingPage.
     */
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("An agent can complete every required traveler-details field for a Regular fare and reach a confirmable booking.")
    public void agentCanCompleteBookingForm_RegularFare() {
        SearchPage searchPage = new SearchPage(page);
        searchPage.searchOneWayFlight("Dhaka", "DAC", "Dubai", "DXB", LocalDate.now().plusMonths(1));
        Assert.assertTrue(searchPage.resultsCount() > 0, "Expected at least one search result");
        searchPage.selectResult(0);

        BookingPage bookingPage = new BookingPage(page);
        Assert.assertTrue(bookingPage.isTravelerDetailsPageDisplayed(),
                "Should reach the traveler details form after selecting a Regular fare");

        bookingPage.fillRequiredTravelerDetails(
                "Mr", "John", "Doe", LocalDate.of(1990, 5, 15),
                "AB1234567", LocalDate.now().minusYears(2), LocalDate.now().plusYears(3),
                "john.doe.qa@example.com", "1712345678");

        Assert.assertTrue(bookingPage.isConfirmBookingButtonVisible(), "Confirm Booking button should be visible");
        Assert.assertTrue(bookingPage.isConfirmBookingButtonEnabled(),
                "Confirm Booking should be enabled once all required traveler details are filled");
    }

    /**
     * Completes the full traveler-details form for a Branded fare (the
     * "View Price" fare-family modal - Economy Lite/Saver/Value/Flex, etc.)
     * and verifies the booking is ready to confirm. Deliberately stops short
     * of clicking "Confirm Booking" - see the class-level warning on
     * BookingPage.
     */
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("An agent can complete every required traveler-details field for a Branded fare and reach a confirmable booking.")
    public void agentCanCompleteBookingForm_BrandedFare() {
        SearchPage searchPage = new SearchPage(page);
        searchPage.searchOneWayFlight("Dhaka", "DAC", "Dubai", "DXB", LocalDate.now().plusMonths(1));
        Assert.assertTrue(searchPage.resultsCount() > 0, "Expected at least one search result");
        searchPage.selectFirstBrandedFare();

        BookingPage bookingPage = new BookingPage(page);
        Assert.assertTrue(bookingPage.isTravelerDetailsPageDisplayed(),
                "Should reach the traveler details form after selecting a Branded fare");

        bookingPage.fillRequiredTravelerDetails(
                "Ms", "Jane", "Smith", LocalDate.of(1992, 8, 21),
                "CD7654321", LocalDate.now().minusYears(1), LocalDate.now().plusYears(4),
                "jane.smith.qa@example.com", "1798765432");

        Assert.assertTrue(bookingPage.isConfirmBookingButtonVisible(), "Confirm Booking button should be visible");
        Assert.assertTrue(bookingPage.isConfirmBookingButtonEnabled(),
                "Confirm Booking should be enabled once all required traveler details are filled");
    }
}
