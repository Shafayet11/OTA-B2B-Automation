package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The flight search form on "/search" (after login). Origin/destination/date
 * are popover pickers, not plain inputs - locators confirmed against the
 * live site. Submitting navigates to "/flights?..." with the results list.
 *
 * Each result card offers two ways to book: a direct "Select" button on a
 * specific booking class ("Regular" fare - {@link #selectResult}), or a
 * "View Price" button that opens a fare-family comparison modal (Economy
 * Lite/Saver/Value/Flex, etc. - "Branded" fare - {@link #selectFirstBrandedFare}).
 * Both proceed to the same traveler-details step (see BookingPage).
 */
public class SearchPage extends BasePage {

    private final Locator originButton;
    private final Locator destinationButton;
    private final Locator dateButton;
    private final Locator locationSearchInput;
    private final Locator searchSubmitButton;
    private final Locator flightDetailsLinks;
    private final Locator selectButtons;
    private final Locator viewPriceButtons;

    public SearchPage(Page page) {
        super(page);
        this.originButton = page.locator("button:has-text('Leaving From')");
        this.destinationButton = page.locator("button:has-text('Going To')");
        this.dateButton = page.locator("button", new Page.LocatorOptions().setHasText(Pattern.compile("^\\d{1,2} \\w{3} \\d{4}")));
        this.locationSearchInput = page.locator("input[placeholder='Search city, airport or code']");
        this.searchSubmitButton = page.locator("button[type='button']")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*Search\\s*$")));
        this.flightDetailsLinks = page.locator("text=Flight Details");
        this.selectButtons = page.locator("button:has-text('Select')");
        this.viewPriceButtons = page.locator("button:has-text('View Price')");
    }

    /** Runs a full one-way search: origin, destination, date, then submits. */
    public void searchOneWayFlight(String originQuery, String originCode,
                                    String destinationQuery, String destinationCode,
                                    LocalDate departureDate) {
        selectOrigin(originQuery, originCode);
        selectDestination(destinationQuery, destinationCode);
        selectDepartureDate(departureDate);
        submitSearch();
    }

    public void selectOrigin(String query, String airportCode) {
        originButton.click();
        // The previous picker's input can stay mounted (hidden), so scope to the one just opened.
        locationSearchInput.last().fill(query);
        page.locator("button", new Page.LocatorOptions().setHasText(airportCode)).first().click();
    }

    public void selectDestination(String query, String airportCode) {
        destinationButton.click();
        locationSearchInput.last().fill(query);
        page.locator("button", new Page.LocatorOptions().setHasText(airportCode)).first().click();
    }

    /**
     * The date picker is a react-day-picker calendar showing the current and
     * next month side by side, but the second (right) panel is rendered
     * aria-hidden until "Next Month" is clicked - only then does it become
     * part of the accessibility tree that getByRole can find. Only supports
     * dates within the next month, which is all these tests need.
     */
    public void selectDepartureDate(LocalDate date) {
        dateButton.click();
        page.locator("button[aria-label='Go to the Next Month']").click();

        // Note: Pattern.quote() wraps the string in Java's \Q..\E syntax, which
        // is meaningless to the JS regex engine Playwright actually matches
        // against - the text here has no regex metacharacters, so build the
        // pattern as a plain string instead.
        String monthFull = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        Pattern target = Pattern.compile(monthFull + " " + date.getDayOfMonth() + "(st|nd|rd|th), " + date.getYear());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(target)).first().click();
        page.keyboard().press("Escape");
    }

    public void submitSearch() {
        searchSubmitButton.click();
        flightDetailsLinks.first().waitFor();
    }

    public int resultsCount() {
        return flightDetailsLinks.count();
    }

    /** Clicks the Nth bookable fare's "Select" button (Regular fare), proceeding to the traveler-details step. */
    public void selectResult(int index) {
        selectButtons.nth(index).click();
    }

    /**
     * Opens the first flight card's "View Price" fare-family modal (Branded
     * fare) and continues with whichever option is pre-selected (the
     * "Recommended" tier), proceeding to the traveler-details step.
     */
    public void selectFirstBrandedFare() {
        viewPriceButtons.first().click();
        page.locator("[role=dialog]", new Page.LocatorOptions().setHasText("Choose a fare option"))
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Continue"))
                .click();
    }
}
