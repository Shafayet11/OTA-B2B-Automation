package com.takeoff.tests;

import com.takeoff.base.AuthenticatedTest;
import com.takeoff.pages.GroupFarePage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;

@Epic("B2B OTA Portal")
@Feature("Group Fare")
public class GroupFareTests extends AuthenticatedTest {

    /**
     * Verifies the flow reaches the passenger-details/booking form after
     * picking a group-fare deal. Deliberately does NOT click "Confirm
     * Booking" - doing so submits a real reservation against live fare
     * inventory and charges the agent's account balance (same category as
     * Booking's "Confirm Booking").
     */
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Description("An agent can pick a group-fare deal and reach the passenger-details booking form.")
    public void agentCanReachGroupFareBookingForm() {
        GroupFarePage groupFarePage = new GroupFarePage(page);
        groupFarePage.open();
        Assert.assertTrue(groupFarePage.hasAvailableDeals(), "Expected at least one group-fare deal listed");

        groupFarePage.bookFirstAvailableDeal();

        Assert.assertTrue(groupFarePage.isPassengerDetailsPageDisplayed(),
                "Should reach the passenger details form after picking a group-fare deal");
        Assert.assertTrue(groupFarePage.isConfirmBookingButtonVisible(),
                "Confirm Booking button should be visible on the final step");
    }

    /**
     * Completes every required passenger-details field for the default
     * single-adult booking and verifies the booking is ready to confirm.
     * Deliberately stops short of clicking "Confirm Booking" - see the
     * class-level warning on GroupFarePage.
     */
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("An agent can complete every required passenger-details field for a group-fare deal and reach a confirmable booking.")
    public void agentCanCompleteGroupFareBookingForm() {
        GroupFarePage groupFarePage = new GroupFarePage(page);
        groupFarePage.open();
        Assert.assertTrue(groupFarePage.hasAvailableDeals(), "Expected at least one group-fare deal listed");

        groupFarePage.bookFirstAvailableDeal();
        Assert.assertTrue(groupFarePage.isPassengerDetailsPageDisplayed(),
                "Should reach the passenger details form after picking a group-fare deal");

        groupFarePage.fillRequiredPassengerDetails(
                "Mr", "John", "Doe", LocalDate.of(1990, 5, 15),
                "AB1234567", LocalDate.now().plusYears(3),
                "john.doe.qa@example.com", "1712345678");
        groupFarePage.agreeToTerms();

        Assert.assertTrue(groupFarePage.isConfirmBookingButtonVisible(), "Confirm Booking button should be visible");
        Assert.assertTrue(groupFarePage.isConfirmBookingButtonEnabled(),
                "Confirm Booking should be enabled once all required passenger details are filled and terms are agreed to");
    }
}
