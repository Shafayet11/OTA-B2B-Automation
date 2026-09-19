package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.time.LocalDate;

/**
 * The traveler-details step at "/flights/form", reached after selecting a
 * fare (Regular or Branded) on the results page. IMPORTANT: automated tests
 * must never call {@link #confirmBooking()} against this environment - it
 * submits a real reservation against live fare inventory and charges the
 * agent's account balance (confirmed with the team; see BookingTests). It's
 * kept here only for manual/exploratory use.
 */
public class BookingPage extends BasePage {

    private final Locator travelerDetailsHeading;
    private final Locator firstNameInput;
    private final Locator lastNameInput;
    private final Locator passportNumberInput;
    private final Locator emailInput;
    private final Locator phoneInput;
    private final Locator confirmBookingButton;

    public BookingPage(Page page) {
        super(page);
        this.travelerDetailsHeading = page.getByText("Provide Traveller Details")
                .or(page.getByText("Provide Traveler Details"));
        this.firstNameInput = page.locator("input[placeholder='As on passport/NID']").first();
        this.lastNameInput = page.locator("input[placeholder='As on passport/NID']").nth(1);
        this.passportNumberInput = fieldByLabel("Passport Number").locator("input");
        this.emailInput = page.locator("input[name='email']");
        this.phoneInput = page.locator("input[name='mobileNo']");
        this.confirmBookingButton = page.locator("button:has-text('Confirm Booking')");
    }

    /** Selecting a fare locks it and redirects asynchronously, so wait rather than checking immediately. */
    public boolean isTravelerDetailsPageDisplayed() {
        try {
            travelerDetailsHeading.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }

    public boolean isConfirmBookingButtonVisible() {
        return confirmBookingButton.isVisible();
    }

    public boolean isConfirmBookingButtonEnabled() {
        return confirmBookingButton.isEnabled();
    }

    public void fillTravelerNames(String firstName, String lastName) {
        firstNameInput.fill(firstName);
        lastNameInput.fill(lastName);
    }

    /**
     * Fills every field the traveler-details form marks required: title,
     * name, date of birth, passport number/issue/expiry dates, and contact
     * email/phone. Gender is derived from title and Country is left at its
     * pre-filled default, so neither is set here. Never touches Confirm
     * Booking - see the class-level warning.
     */
    public void fillRequiredTravelerDetails(String title, String firstName, String lastName, LocalDate dateOfBirth,
                                             String passportNumber, LocalDate passportIssueDate,
                                             LocalDate passportExpiryDate, String email, String phoneNumber) {
        selectDropdown("Title", title);
        // Gender is derived from Title (e.g. "Mr" -> "Male") and the field is
        // disabled immediately after, so it's never set independently here.
        fillTravelerNames(firstName, lastName);
        pickDate(fieldByLabel("Date of birth").locator("button"), dateOfBirth);
        passportNumberInput.fill(passportNumber);
        pickDate(fieldByLabel("Passport Issue Date").locator("button"), passportIssueDate);
        pickDate(fieldByLabel("Passport Expiry Date").locator("button"), passportExpiryDate);
        emailInput.fill(email);
        phoneInput.fill(phoneNumber);
    }

    /**
     * Do not call from automated tests: submits a real reservation against
     * live fare inventory and charges the agent's account balance.
     */
    public void confirmBooking() {
        confirmBookingButton.click();
    }
}
