package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Admin portal &gt; Balance Management &gt; Topup &gt; View Request (URL
 * {@code /apis/agentbalance-list/0} - an internal/generic slug, so this class
 * is named after what admins call it in the sidebar instead). Lists pending
 * top-up requests and lets an admin approve, reject, or hold each one.
 *
 * <p>IMPORTANT: automated tests must never call {@link #approve(String)} -
 * approving credits the agent's account balance for real (confirmed with the
 * team, same category as {@link BookingPage#confirmBooking()}). It's kept
 * here only for manual/exploratory use. Tests should instead fill the
 * request's Admin Reference/Remarks and assert the Approve button is
 * enabled via {@link #isApproveButtonEnabled()}.
 *
 * <p>Reject and Pending are different: unlike Approve, neither moves money -
 * they only change the request's status (confirmed with the team) - so
 * {@link #reject(String)} and {@link #setPending(String)} are fine for tests
 * to call directly, unlike {@link #approve(String)}.
 */
public class TopupApprovalPage extends BasePage {

    private final Locator balanceManagementMenu;
    private final Locator topupMenu;
    private final Locator viewRequestMenu;
    private final Locator adminReferenceInput;
    private final Locator remarksInput;
    private final Locator approveButton;
    private final Locator rejectButton;
    private final Locator pendingButton;
    private final Locator pinInput;
    private final Locator pinSaveButton;
    private final Locator successToast;

    public TopupApprovalPage(Page page) {
        super(page);
        this.balanceManagementMenu = page.locator("text=Balance Management").first();
        this.topupMenu = page.locator("text=Topup").first();
        this.viewRequestMenu = page.locator("text=View Request").first();
        this.adminReferenceInput = page.locator("#inp-reference-no");
        this.remarksInput = page.locator("#inp-remarks");
        this.approveButton = page.locator("button.btn-approve");
        this.rejectButton = page.locator("button.btn-reject");
        this.pendingButton = page.locator("button.btn-pending");
        this.pinInput = page.locator("input[placeholder='PIN']");
        this.pinSaveButton = page.locator("button:has-text('SAVE')");
        this.successToast = page.getByText("Top-up Status successfully Changed..");
    }

    /**
     * Expands "Balance Management" &gt; "Topup" in the sidebar and opens
     * "View Request". A first login can land on an "Authenticator Setup" QR
     * screen instead of the dashboard, but that only occupies the main
     * content pane - the sidebar underneath stays interactive, so this
     * navigates straight through it without completing 2FA enrollment.
     */
    public void open() {
        balanceManagementMenu.click();
        topupMenu.click();
        viewRequestMenu.click();
    }

    /**
     * Opens the detail view for the most recent (topmost - the list is
     * sorted newest-first) request of the given deposit type submitted by
     * the given agent email, while still in "Requested" status.
     */
    public void openLatestRequest(String depositType, String requestedByEmail) {
        Locator row = page.locator("tr:has-text('" + depositType + "')"
                + ":has-text('Requested')"
                + ":has-text('" + requestedByEmail + "')").first();
        row.locator("button:has-text('View')").click();
    }

    public void fillApprovalDetails(String adminReference, String remarks) {
        adminReferenceInput.fill(adminReference);
        remarksInput.fill(remarks);
    }

    public boolean isApproveButtonEnabled() {
        return approveButton.isEnabled();
    }

    public boolean isRejectButtonEnabled() {
        return rejectButton.isEnabled();
    }

    public boolean isPendingButtonEnabled() {
        return pendingButton.isEnabled();
    }

    /**
     * NOT called by automated tests - see class javadoc. Clicks Approve,
     * enters the admin PIN in the confirmation dialog it opens, and saves.
     */
    public void approve(String adminPin) {
        approveButton.click();
        pinInput.fill(adminPin);
        pinSaveButton.click();
    }

    /** Clicks Reject, enters the admin PIN in the confirmation dialog it opens, and saves. */
    public void reject(String adminPin) {
        rejectButton.click();
        pinInput.fill(adminPin);
        pinSaveButton.click();
    }

    /** Clicks Pending, enters the admin PIN in the confirmation dialog it opens, and saves. */
    public void setPending(String adminPin) {
        pendingButton.click();
        pinInput.fill(adminPin);
        pinSaveButton.click();
    }

    public boolean isSuccessToastVisible() {
        try {
            successToast.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }
}
