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

import java.util.Collections;
import java.util.List;

@Epic("B2B OTA Portal")
@Feature("Group Fare")
public class GroupFareTests extends AuthenticatedTest {

    /**
     * Selects exactly 12 Adults and 5 Children (17 total) on a group-fare
     * deal - Adult age 12+, Child age 2-11, per {@link GroupFarePage}'s
     * sample-age constants - then fills every passenger's details via the
     * "Fill passengers from a spreadsheet" upload: downloading the real
     * Template, filling a row per passenger without touching its
     * structure/headers, and uploading it. Verifies the app maps and
     * classifies all 17 passengers correctly (12 Adult / 5 Child, no
     * misclassification, no error/validation message), then clicks "Confirm
     * Booking", verifies the booking lands on the Booking List as "Booked"
     * with 17 passengers, and finally verifies that same booking can be
     * found by searching the Booking List by its Booking ID - by explicit
     * user instruction, unlike every other test in this project. CAUTION:
     * this submits a real reservation and charges the agent's account
     * balance on every run - see the class-level warning on GroupFarePage.
     */
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("An agent can book a 12-adult/5-child group fare deal, have all 17 passengers correctly mapped and classified from an uploaded spreadsheet, and confirm the booking.")
    public void agentCanCompleteGroupFareBookingForm_BulkPassengersViaSpreadsheet() {
        final int requestedAdults = 12;
        final int requestedChildren = 5;
        final int totalPassengers = requestedAdults + requestedChildren;

        GroupFarePage groupFarePage = new GroupFarePage(page);
        groupFarePage.open();
        Assert.assertTrue(groupFarePage.hasAvailableDeals(), "Expected at least one group-fare deal listed");

        groupFarePage.bookFirstAvailableDealWithPassengers(requestedAdults, requestedChildren);
        Assert.assertTrue(groupFarePage.isPassengerDetailsPageDisplayed(),
                "Should reach the passenger details form after picking a group-fare deal");

        Assert.assertEquals(groupFarePage.adultCount(), requestedAdults, "Expected exactly 12 adults to be selected");
        Assert.assertEquals(groupFarePage.childCount(), requestedChildren, "Expected exactly 5 children to be selected");

        groupFarePage.fillPassengerDetailsFromSpreadsheet(requestedAdults, requestedChildren);
        Assert.assertEquals(groupFarePage.firstPassengerFirstName(), "GroupAdult1",
                "Expected the first passenger's name to be auto-filled from the uploaded spreadsheet");

        // Total passenger count = 17, all mapped and marked complete.
        List<String> passengerSummaries = groupFarePage.passengerSummaries();
        Assert.assertEquals(passengerSummaries.size(), totalPassengers, "Expected one passenger accordion item per uploaded row");
        Assert.assertEquals(groupFarePage.completedPassengerCount(), totalPassengers,
                "Expected every uploaded passenger to be marked complete");
        Assert.assertTrue(passengerSummaries.get(0).contains("GroupAdult1"), "First passenger should be GroupAdult1");
        Assert.assertTrue(passengerSummaries.get(requestedAdults - 1).contains("GroupAdult" + requestedAdults),
                "Last adult passenger should be GroupAdult" + requestedAdults);
        Assert.assertTrue(passengerSummaries.get(requestedAdults).contains("GroupChild1"), "First child passenger should be GroupChild1");
        Assert.assertTrue(passengerSummaries.get(totalPassengers - 1).contains("GroupChild" + requestedChildren),
                "Last child passenger should be GroupChild" + requestedChildren);

        // Adult count = 12, Child count = 5, no passenger misclassified by age.
        List<String> typeBadges = groupFarePage.passengerTypeBadges();
        Assert.assertEquals(typeBadges.size(), totalPassengers, "Expected a type badge per passenger");
        Assert.assertEquals(typeBadges.subList(0, requestedAdults), Collections.nCopies(requestedAdults, "Adult"),
                "Expected the first 12 passengers to be classified as Adult");
        Assert.assertEquals(typeBadges.subList(requestedAdults, totalPassengers), Collections.nCopies(requestedChildren, "Child"),
                "Expected the last 5 passengers to be classified as Child");

        // No validation/error message displayed for these valid passenger ages.
        List<String> statusMessages = groupFarePage.statusMessages();
        for (String message : statusMessages) {
            String lower = message.toLowerCase();
            Assert.assertFalse(lower.contains("error") || lower.contains("invalid") || lower.contains("mismatch"),
                    "Expected no validation/error message, but found: " + message);
        }

        groupFarePage.agreeToTerms();

        Assert.assertTrue(groupFarePage.isConfirmBookingButtonVisible(), "Confirm Booking button should be visible");
        Assert.assertTrue(groupFarePage.isConfirmBookingButtonEnabled(),
                "Confirm Booking should be enabled once every passenger's details are filled and terms are agreed to");

        // Submits a real reservation and charges the agent's account balance - see the CAUTION on GroupFarePage.
        groupFarePage.confirmBooking();

        Assert.assertTrue(groupFarePage.isBookingListPageDisplayed(),
                "Should redirect to the Booking List after a successful Confirm Booking");
        Assert.assertEquals(groupFarePage.latestBookingPassengerCount(), String.valueOf(totalPassengers),
                "Expected the new booking's passenger count to be 17");
        Assert.assertEquals(groupFarePage.latestBookingStatus(), "Booked", "Expected the new booking to be marked Booked");
        String bookingId = groupFarePage.latestBookingId();
        Assert.assertTrue(bookingId.startsWith("GFB-"), "Expected a Group Fare booking ID, but got: " + bookingId);

        // Confirm the same booking can be found by searching the Booking List by its ID.
        groupFarePage.searchBookingListByBookingId(bookingId);
        Assert.assertEquals(groupFarePage.bookingListRowCount(), 1,
                "Expected exactly one Booking List row when filtered by this booking's ID");
        Assert.assertEquals(groupFarePage.latestBookingId(), bookingId, "Search result's Booking ID should match what was searched for");
        Assert.assertEquals(groupFarePage.latestBookingPassengerCount(), String.valueOf(totalPassengers),
                "Expected the searched booking's passenger count to still be 17");
        Assert.assertEquals(groupFarePage.latestBookingStatus(), "Booked", "Expected the searched booking to be marked Booked");
    }
}
