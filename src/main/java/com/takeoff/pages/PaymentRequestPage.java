package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * "Payment Request" ({@code /topup-request}), reached via the sidebar. An
 * agent submits a deposit/top-up request here for someone else to review -
 * unlike Booking's "Confirm Booking", clicking {@link #submit()} does not
 * move money by itself, so automated tests may click it freely, but it does
 * create a real request record every time.
 *
 * <p>The "Submit Request" tab offers 6 deposit types via {@link DepositType},
 * each with a different field set. Cheque, Bank Deposit and Bank Transfer
 * all require picking a "Deposited In" (and Bank Transfer also "Deposited
 * From") bank account from a dropdown that is empty in this environment
 * despite "Partner Bank Details" listing valid accounts - confirmed via the
 * underlying native {@code <select>} also having zero {@code <option>}s, not
 * just a slow-to-render list. That's an app/data bug to raise with the team,
 * not a test issue; those 3 fill methods are implemented and ready to use
 * once it's fixed, but their tests are disabled until then (see
 * PaymentRequestTests).
 */
public class PaymentRequestPage extends BasePage {

    public enum DepositType {
        CHEQUE("Cheque"), BANK_DEPOSIT("Bank Deposit"), BANK_TRANSFER("Bank Transfer"),
        CASH("Cash"), BKASH("Bkash"), NAGAD("Nagad");

        private final String label;

        DepositType(String label) {
            this.label = label;
        }
    }

    private final Locator amountInput;
    private final Locator referenceInput;
    private final Locator bankChargeInput;
    private final Locator branchNameInput;
    private final Locator transactionIdInput;
    private final Locator attachmentInput;
    private final Locator submitButton;
    private final Locator successToast;

    public PaymentRequestPage(Page page) {
        super(page);
        this.amountInput = page.locator("input[name='amount']");
        this.referenceInput = page.locator("input[name='reference']");
        this.bankChargeInput = page.locator("input[name='bankChargeAdmin']");
        this.branchNameInput = page.locator("input[name='branchName']");
        this.transactionIdInput = page.locator("input[name='transactionId']");
        this.attachmentInput = page.locator("input[type='file']");
        this.submitButton = page.locator("button[type='submit']:has-text('Submit')");
        this.successToast = page.getByText("Deposit request successfully sent");
    }

    /** Navigates here from the sidebar (present on every authenticated page). */
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
    public boolean isSuccessToastVisible() {
        try {
            successToast.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
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
}
