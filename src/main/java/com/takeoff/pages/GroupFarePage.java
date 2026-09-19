package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.time.LocalDate;

/**
 * "Group Fare" ({@code /group-fares}), reached via the sidebar. Lists
 * discounted group-fare deals under its default "Group Fare" tab (a
 * "Booking List" tab sits alongside it, out of scope here). Picking a deal's
 * "Book Now" opens a "Select Passengers" modal (adult/child counts default to
 * 1/0, which is enough for a single-passenger booking), then "Add Passenger
 * Info" reaches {@code /group-fares/form} - the same kind of traveler-details
 * step as {@link BookingPage}, ending in a "Confirm Booking" that moves money
 * and books against live inventory. IMPORTANT: automated tests must never
 * call {@link #confirmBooking()} against this environment (same convention as
 * {@code BookingPage.confirmBooking()}); it's kept here only for
 * manual/exploratory use.
 */
public class GroupFarePage extends BasePage {

    private final Locator bookNowButtons;
    private final Locator addPassengerInfoButton;
    private final Locator firstNameInput;
    private final Locator lastNameInput;
    private final Locator emailInput;
    private final Locator phoneInput;
    private final Locator passportNumberInput;
    private final Locator agreeTermsCheckbox;
    private final Locator confirmBookingButton;

    public GroupFarePage(Page page) {
        super(page);
        this.bookNowButtons = page.locator("button:has-text('Book Now')");
        this.addPassengerInfoButton = page.locator("button:has-text('Add Passenger Info')");
        this.firstNameInput = page.locator("input[name='passengerInfoes.0.first']");
        this.lastNameInput = page.locator("input[name='passengerInfoes.0.last']");
        this.emailInput = page.locator("input[name='passengerInfoes.0.email']");
        this.phoneInput = page.locator("input[name='passengerInfoes.0.phone']");
        this.passportNumberInput = page.locator("input[name='passengerInfoes.0.documentNumber']");
        this.agreeTermsCheckbox = page.locator("button[role='checkbox']");
        this.confirmBookingButton = page.locator("button:has-text('Confirm Booking')");
    }

    /** Navigates here from the sidebar (present on every authenticated page). Lands on the "Group Fare" tab. */
    public void open() {
        page.getByText("Group Fare", new Page.GetByTextOptions().setExact(true)).first().click();
        page.waitForURL("**/group-fares", new Page.WaitForURLOptions().setTimeout(15000));
    }

    public boolean hasAvailableDeals() {
        try {
            bookNowButtons.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }

    /** Books the first listed deal and accepts the default passenger count (1 adult) from the resulting modal. */
    public void bookFirstAvailableDeal() {
        bookNowButtons.first().click();
        addPassengerInfoButton.click();
        page.waitForURL("**/group-fares/form", new Page.WaitForURLOptions().setTimeout(15000));
    }

    public boolean isPassengerDetailsPageDisplayed() {
        try {
            page.getByText("Provide Passenger Details").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }

    /**
     * Fills every field the passenger-details form marks required: title,
     * name, date of birth, passport number/expiry date, and contact
     * email/phone (no separate "Passport Issue Date" here, unlike Booking's
     * traveler form). Gender is derived from title and disabled immediately
     * after, and Nationality/Issuing Country are left at their pre-filled
     * default, so none of those are set here. Never touches Confirm Booking.
     */
    public void fillRequiredPassengerDetails(String title, String firstName, String lastName, LocalDate dateOfBirth,
                                              String passportNumber, LocalDate passportExpiryDate,
                                              String email, String phoneNumber) {
        selectDropdown("Title", title);
        firstNameInput.fill(firstName);
        lastNameInput.fill(lastName);
        pickDate(fieldByLabel("Date of birth").locator("button").first(), dateOfBirth);
        passportNumberInput.fill(passportNumber);
        pickDate(fieldByLabel("Passport Expiry Date").locator("button").first(), passportExpiryDate);
        emailInput.fill(email);
        phoneInput.fill(phoneNumber);
    }

    public void agreeToTerms() {
        agreeTermsCheckbox.click();
    }

    public boolean isConfirmBookingButtonVisible() {
        return confirmBookingButton.isVisible();
    }

    public boolean isConfirmBookingButtonEnabled() {
        return confirmBookingButton.isEnabled();
    }

    /**
     * Do not call from automated tests: submits a real reservation against
     * live fare inventory and charges the agent's account balance.
     */
    public void confirmBooking() {
        confirmBookingButton.click();
    }
}
