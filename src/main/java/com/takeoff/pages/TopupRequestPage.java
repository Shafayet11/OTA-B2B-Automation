package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Both sides of the top-up flow: the B2B agent's "Payment Request"
 * ({@code /topup-request}, reached via the sidebar) where a deposit/top-up
 * request is submitted, and the Admin portal's Balance Management &gt;
 * Topup &gt; View Request ({@code /apis/agentbalance-list/0} - an
 * internal/generic slug, so this class is named after what admins call it in
 * the sidebar instead) where that request is reviewed. Merged into one page
 * object since the test flow drives both back-to-back on the same {@code
 * Page} (agent submits, then the same browser tab navigates to the Admin
 * portal and logs in there).
 *
 * <p><b>Agent side:</b> the "Submit Request" tab offers 6 deposit types via
 * {@link DepositType}, each with a different field set. Cheque, Bank Deposit
 * and Bank Transfer all require picking a "Deposited In" (and Bank Transfer
 * also "Deposited From") bank account from a dropdown that is empty in this
 * environment despite "Partner Bank Details" listing valid accounts -
 * confirmed via the underlying native {@code <select>} also having zero
 * {@code <option>}s, not just a slow-to-render list. That's an app/data bug
 * to raise with the team, not a test issue; those 3 fill methods are
 * implemented and ready to use once it's fixed, but their tests are disabled
 * until then (see TopupRequestTests). Unlike Booking's "Confirm Booking",
 * clicking {@link #submit()} does not move money by itself, so automated
 * tests may click it freely, but it does create a real request record every
 * time.
 *
 * <p><b>Admin side:</b> lists pending top-up requests and lets an admin
 * approve, reject, or hold each one. Approving credits the agent's account
 * balance for real (confirmed with the team, same category as {@link
 * BookingPage#confirmBooking()}) - {@link #approve(String)} is called
 * directly by {@code TopupRequestTests} (by explicit user instruction, so
 * that test can then verify the credit lands on the agent's Ledger Report),
 * unlike {@code BookingPage.confirmBooking()} which still isn't called from
 * automated tests.
 * Reject and Pending are different: unlike Approve, neither moves money -
 * they only change the request's status (confirmed with the team) - so
 * {@link #reject(String)} and {@link #setPending(String)} are fine for tests
 * to call directly, unlike {@link #approve(String)}.
 */
public class TopupRequestPage extends BasePage {

    public enum DepositType {
        CHEQUE("Cheque"), BANK_DEPOSIT("Bank Deposit"), BANK_TRANSFER("Bank Transfer"),
        CASH("Cash"), BKASH("Bkash"), NAGAD("Nagad");

        private final String label;

        DepositType(String label) {
            this.label = label;
        }
    }

    // Agent side ("Payment Request" / /topup-request)
    private final Locator amountInput;
    private final Locator referenceInput;
    private final Locator bankChargeInput;
    private final Locator branchNameInput;
    private final Locator transactionIdInput;
    private final Locator attachmentInput;
    private final Locator submitButton;
    private final Locator submitSuccessToast;

    // Admin side (Balance Management > Topup > View Request)
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
    private final Locator statusChangeSuccessToast;

    public TopupRequestPage(Page page) {
        super(page);

        this.amountInput = page.locator("input[name='amount']");
        this.referenceInput = page.locator("input[name='reference']");
        this.bankChargeInput = page.locator("input[name='bankChargeAdmin']");
        this.branchNameInput = page.locator("input[name='branchName']");
        this.transactionIdInput = page.locator("input[name='transactionId']");
        this.attachmentInput = page.locator("input[type='file']");
        this.submitButton = page.locator("button[type='submit']:has-text('Submit')");
        this.submitSuccessToast = page.getByText("Deposit request successfully sent");

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
        this.statusChangeSuccessToast = page.getByText("Top-up Status successfully Changed..");
    }

    /** Navigates to "Payment Request" from the sidebar (present on every authenticated agent page). */
    public void open() {
        page.getByText("Payment Request", new Page.GetByTextOptions().setExact(true)).first().click();
        page.waitForURL("**/topup-request", new Page.WaitForURLOptions().setTimeout(15000));
    }

    public void selectDepositType(DepositType type) {
        page.locator("text=" + type.label).first().click();
    }

    /** The attachment is required for every deposit type, despite having no visible asterisk on the heading. */
    public void attachReceipt(Path filePath) {
        attachmentInput.setInputFiles(filePath);
    }

    public void submit() {
        submitButton.click();
    }

    /** Submitting resets the form back to its default state, so wait rather than checking immediately. */
    public boolean isSubmitSuccessToastVisible() {
        try {
            submitSuccessToast.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }

    public void fillCash(String amount, String branchName, String reference, Path attachment) {
        selectDepositType(DepositType.CASH);
        amountInput.fill(amount);
        branchNameInput.fill(branchName);
        referenceInput.fill(reference);
        attachReceipt(attachment);
    }

    public void fillBkash(String amount, LocalDate depositDate, String transactionId, Path attachment) {
        selectDepositType(DepositType.BKASH);
        amountInput.fill(amount);
        pickDate(fieldByLabel("Deposit Date").locator("button"), depositDate);
        transactionIdInput.fill(transactionId);
        attachReceipt(attachment);
    }

    public void fillNagad(String amount, LocalDate depositDate, String transactionId, Path attachment) {
        selectDepositType(DepositType.NAGAD);
        amountInput.fill(amount);
        pickDate(fieldByLabel("Deposit Date").locator("button"), depositDate);
        transactionIdInput.fill(transactionId);
        attachReceipt(attachment);
    }

    /**
     * Ready for when "Deposited In" is fixed to list the partner bank
     * accounts (see class-level note) - not currently usable end-to-end.
     */
    public void fillCheque(String amount, LocalDate depositedDate, String chequeNo, String chequeBank,
                            String depositedIn, String reference, String bankCharge, Path attachment) {
        selectDepositType(DepositType.CHEQUE);
        amountInput.fill(amount);
        pickDate(fieldByLabel("Deposited Date").locator("button"), depositedDate);
        fieldByLabel("Cheque No").locator("input").fill(chequeNo);
        fieldByLabel("Cheque Bank").locator("input").fill(chequeBank);
        selectDropdown("Deposited In", depositedIn);
        referenceInput.fill(reference);
        bankChargeInput.fill(bankCharge);
        attachReceipt(attachment);
    }

    /** Ready for when "Deposited In" is fixed (see class-level note) - not currently usable end-to-end. */
    public void fillBankDeposit(String amount, LocalDate depositedDate, String depositedIn, String reference,
                                 String bankCharge, Path attachment) {
        selectDepositType(DepositType.BANK_DEPOSIT);
        amountInput.fill(amount);
        pickDate(fieldByLabel("Deposited Date").locator("button"), depositedDate);
        selectDropdown("Deposited In", depositedIn);
        referenceInput.fill(reference);
        bankChargeInput.fill(bankCharge);
        attachReceipt(attachment);
    }

    /** Ready for when "Deposited In"/"Deposited From" are fixed (see class-level note) - not currently usable end-to-end. */
    public void fillBankTransfer(String amount, LocalDate depositedDate, String depositedIn, String depositedFrom,
                                  String reference, String bankCharge, Path attachment) {
        selectDepositType(DepositType.BANK_TRANSFER);
        amountInput.fill(amount);
        pickDate(fieldByLabel("Deposited Date").locator("button"), depositedDate);
        selectDropdown("Deposited In", depositedIn);
        selectDropdown("Deposited From", depositedFrom);
        referenceInput.fill(reference);
        bankChargeInput.fill(bankCharge);
        attachReceipt(attachment);
    }

    /**
     * Expands "Balance Management" &gt; "Topup" in the sidebar and opens
     * "View Request". A first login can land on an "Authenticator Setup" QR
     * screen instead of the dashboard, but that only occupies the main
     * content pane - the sidebar underneath stays interactive, so this
     * navigates straight through it without completing 2FA enrollment.
     */
    public void openAdminRequestList() {
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
     * CAUTION: credits the agent's account balance for real every time it's
     * called - see class javadoc. Clicks Approve, enters the admin PIN in
     * the confirmation dialog it opens, and saves.
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

    public boolean isStatusChangeSuccessToastVisible() {
        try {
            statusChangeSuccessToast.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }
}
