package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.takeoff.config.ConfigManager;

/**
 * Covers the rest of ticket issuance (picking up right after
 * {@link BookingPage#confirmBooking()}) and the full Refund flow: the
 * "Refund" button on an issued ticket's ticket-view, the request form at
 * "/booking/refund" (segment/passenger selection, agent remarks), the
 * resulting quotation at "/booking/refund/view", and its Accept/Reject
 * confirmation dialog.
 *
 * <p>Both the Review step's "Confirm Booking" and "Issue Ticket" show a
 * loading spinner that can stay visible well past a minute even though the
 * backend has already finished the step - waiting on in-page state after
 * those clicks is unreliable (confirmed by repeated exploration: the booking/
 * ticket existed on reload long before the UI ever stopped spinning). The
 * methods below poll the booking list / ticket-view directly instead, which
 * always reflects the real state.
 *
 * <p>Automatic vs. manual (admin-created) quotation isn't predictable in
 * advance (confirmed with the user) - every quotation observed during
 * exploration came back automatic, so only that path is exercised here.
 */
public class RefundPage extends BasePage {

    private final Locator refundButton;
    private final Locator getQuotationButton;
    private final Locator refundNowButton;
    private final Locator agentRemarksField;

    public RefundPage(Page page) {
        super(page);
        this.refundButton = page.locator("button:has-text('Refund')");
        this.getQuotationButton = page.locator("button:has-text('Get Quotation')");
        this.refundNowButton = page.locator("button:has-text('Refund Now')");
        this.agentRemarksField = page.locator("textarea[placeholder='Anything the ticketing desk should know about this request']");
    }

    /**
     * Drives whatever sequence of steps sits between the simple Traveller
     * form and the Hold booking - a Review step (Terms & Conditions
     * checkbox) and/or a fuller "Provide Traveller Details" step (document/
     * passport fields - the data already carried over from
     * {@link BookingPage#fillRequiredTravelerDetails} is sufficient, nothing
     * else needs filling there).
     *
     * <p>Exploration found this sequence genuinely isn't consistent run to
     * run - sometimes both steps appear in order, sometimes one is skipped
     * entirely straight to the Hold booking, and a "Confirm Booking" click
     * can take well over a minute to render its result or not register at
     * all. Rather than assume any fixed order, this just checks the Terms
     * checkbox when a Review step is showing and keeps clicking whatever
     * "Confirm Booking" button is present and enabled until the URL reaches
     * booking-view (the Hold booking actually exists on the backend by then -
     * confirmed by exploration).
     */
    public void agreeAndConfirmReview() {
        Locator reviewCheckbox = page.getByText("I confirm that all information", new Page.GetByTextOptions().setExact(false));
        Locator confirmButton = page.locator("button:has-text('Confirm Booking')").last();

        for (int attempt = 0; attempt < 15 && !page.url().contains("booking-view"); attempt++) {
            if (reviewCheckbox.count() > 0) {
                page.getByRole(AriaRole.CHECKBOX).first().click();
            }
            if (confirmButton.count() > 0 && confirmButton.isEnabled()) {
                confirmButton.click();
            }
            page.waitForTimeout(10000);
        }

        if (!page.url().contains("booking-view")) {
            throw new IllegalStateException("Never reached the Hold booking (booking-view) after confirming the traveller steps");
        }
    }

    /**
     * Extracts the booking reference (the {@code utid} query param used on
     * booking-view, ticket-view, and every refund URL) from the current page
     * - call right after {@link #agreeAndConfirmReview()}, which always lands
     * on booking-view with it already in the URL.
     */
    public String currentBookingReference() {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("utid=([^&]+)").matcher(page.url());
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("No utid query param in current URL: " + page.url());
    }

    /** Opens an already-issued ticket's copy directly by its booking reference. */
    public void openTicketView(String bookingReference) {
        page.navigate(ConfigManager.b2bUrl() + "ticket-view?utid=" + bookingReference + "&status=Confirmed");
    }

    /** Opens the Refund Ticket Request form directly for an issued, Eligible ticket. */
    public void openRefundRequestForm(String bookingReference) {
        page.navigate(ConfigManager.b2bUrl() + "booking/refund?utid=" + bookingReference);
    }

    /**
     * Agrees to T&C and issues the ticket for real with Full Payment (the
     * pre-selected default) - charges the agent's account balance. Callers
     * should poll {@link #waitForTicketIssued} rather than wait on this page.
     */
    public void agreeAndIssueTicket() {
        page.getByRole(AriaRole.CHECKBOX).first().click();
        page.locator("button:has-text('Issue Ticket')").click();
    }

    /** Polls ticket-view until the ticket shows status "Issued", confirming issuance actually completed. */
    public void waitForTicketIssued(String bookingReference) {
        for (int attempt = 0; attempt < 15; attempt++) {
            page.waitForTimeout(5000);
            openTicketView(bookingReference);
            if (page.getByText("Issued", new Page.GetByTextOptions().setExact(true)).count() > 0) {
                return;
            }
        }
        throw new IllegalStateException("Ticket " + bookingReference + " never reached Issued status");
    }

    public void clickRefund() {
        refundButton.click();
    }

    /**
     * Selects every available segment/passenger checkbox on the Refund Ticket
     * Request form. The passenger row's data (and its checkbox) can finish
     * rendering slightly after the segment row's, so this waits for the
     * "Eligible" status to show before counting checkboxes - counting too
     * early can miss the passenger checkbox entirely and leave "Get
     * Quotation" permanently disabled.
     */
    public void selectAllForRefund() {
        page.waitForSelector("text=Eligible", new Page.WaitForSelectorOptions().setTimeout(30000));
        Locator checkboxes = page.getByRole(AriaRole.CHECKBOX);
        int count = checkboxes.count();
        for (int i = 0; i < count; i++) {
            // Checking one box can re-render the form (e.g. segment selection
            // affecting passenger eligibility) - a pause between clicks avoids
            // acting on a stale element once that happens.
            checkboxes.nth(i).click();
            page.waitForTimeout(1000);
        }
    }

    public void fillAgentRemarks(String remarks) {
        agentRemarksField.fill(remarks);
    }

    public boolean isEligible() {
        return page.getByText("Eligible", new Page.GetByTextOptions().setExact(true)).count() > 0;
    }

    /**
     * Submits the refund request and waits for a quotation - automatic or
     * manual, whichever this ticket/fare triggers. Can take well over a
     * minute even when automatic (see class javadoc).
     */
    public void getQuotation() {
        getQuotationButton.click();
        page.waitForSelector("text=Quotation Summary", new Page.WaitForSelectorOptions().setTimeout(90000));
    }

    public boolean isQuotationSummaryVisible() {
        return page.getByText("Quotation Summary").isVisible();
    }

    /**
     * Accepts the quotation for real - returns the ticket to the airline
     * against its fare rules and credits the refundable amount back
     * (confirmed with the user; same category as Booking's "Confirm
     * Booking").
     */
    public void acceptQuotation() {
        page.getByRole(AriaRole.CHECKBOX).first().click();
        refundNowButton.click();
        page.locator("[role=dialog] button:has-text('Accept')").click();
        // The confirmation modal shows its own loading spinner before redirecting to
        // refund-list (often tagged ?tab=Refunded) - wait for that rather than the click.
        page.waitForURL(url -> url.contains("refund-list"), new Page.WaitForURLOptions().setTimeout(60000));
    }

    /**
     * Rejects the quotation instead of accepting it - doesn't move money. The
     * ticket becomes Eligible again afterward and a fresh request can be made
     * (confirmed by exploration).
     */
    public void rejectQuotation() {
        page.getByRole(AriaRole.CHECKBOX).first().click();
        refundNowButton.click();
        page.locator("[role=dialog] button:has-text('Reject')").click();
        page.waitForURL(url -> url.contains("refund-list"), new Page.WaitForURLOptions().setTimeout(60000));
    }
}
