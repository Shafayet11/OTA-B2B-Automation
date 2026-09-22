package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Admin portal &gt; B2B Management &gt; Requested Agent / Approved Agent. An
 * admin reviews a newly self-registered agency here (see {@link RegisterPage}).
 * Approve only changes the request's status - unlike Balance Management's
 * topup Approve (see {@link TopupApprovalPage}), it doesn't move money - so
 * tests click through for real (confirmed with the user).
 *
 * <p>Approve fails with a "Postal Code Is Required" validation unless the
 * agent's info is edited first to fill in fields the public {@code /signup}
 * form never collects: Category, Postal Code, Country, City, Zone and
 * Account Manager, plus Currency. These are cascading {@code ng-select}
 * dropdowns - Country must be picked before City, and Zone before Account
 * Manager, or the later one shows "No items found" (confirmed with the
 * user).
 */
public class AgentApprovalPage extends BasePage {

    private final Locator emailFilterInput;
    private final Locator searchButton;
    private final Locator approveButton;
    private final Locator editButton;
    private final Locator confirmButton;
    private final Locator saveButton;
    private final Locator postalCodeInput;
    private final Locator actionsToggle;
    private final Locator activeMenuItem;

    public AgentApprovalPage(Page page) {
        super(page);
        this.emailFilterInput = page.locator("input[placeholder='Input Email']");
        this.searchButton = page.locator("button:has-text('SEARCH')").first();
        this.approveButton = page.locator("button.btn-success").first();
        this.editButton = page.locator("button.btn-info").first();
        // Angular leaves a previous "Change Status" dialog's Confirm button hidden in the
        // DOM rather than removing it, so a fresh dialog's button is the LAST match, not first.
        this.confirmButton = page.locator("button:has-text('CONFIRM')").last();
        this.saveButton = page.locator("button:has-text('SAVE')").first();
        this.postalCodeInput = page.locator("input[placeholder='Postal Code']");
        this.actionsToggle = page.locator("text=ACTIONS").first();
        this.activeMenuItem = page.getByText("Active", new Page.GetByTextOptions().setExact(true)).first();
    }

    /** Expands "B2B Management" and opens "Requested Agent" - both first-time-only in the sidebar. */
    public void openRequested() {
        page.locator("text=B2B Management").first().click();
        page.locator("text=Requested Agent").first().click();
    }

    /** "B2B Management" is already expanded once {@link #openRequested()} has run. */
    public void openApproved() {
        page.locator("text=Approved Agent").first().click();
    }

    /** Filters the current list (Requested or Approved) down to the one agent with this email. */
    public void searchByEmail(String email) {
        emailFilterInput.fill(email);
        searchButton.click();
    }

    public void openEdit() {
        editButton.click();
    }

    /**
     * Fills the fields Approve needs that {@code /signup} never collects.
     * Category, Country, City, Zone and Account Manager are picked by their
     * first available real option (this environment only has one Account
     * Manager regardless) - order matters, see class javadoc.
     */
    public void fillMandatoryFieldsForApproval(String postalCode) {
        selectNgOption("categoryIds", "B2B");
        postalCodeInput.fill(postalCode);
        selectNgOptionBySearch("countryId", "Bangladesh");
        selectFirstRealNgOption("cityId");
        selectFirstRealNgOption("zoneId");
        selectFirstRealNgOption("accountManagerId");
        selectNgOption("currencyId", "BDT");
    }

    public void save() {
        saveButton.click();
    }

    /** Clicks Approve and confirms the "Change Status" dialog it opens. */
    public void approve() {
        approveButton.click();
        confirmChangeStatus();
    }

    /**
     * Opens the card's "ACTIONS" menu and clicks "Active", confirming the
     * "Are You Sure? Want to Activated Agent ..." dialog it opens - a
     * different dialog (OK/CANCEL) from Approve/Reject/Pending's "Change
     * Status" (CONFIRM/CLOSE) one.
     */
    public void activate() {
        actionsToggle.click();
        activeMenuItem.click();
        page.locator("button:has-text('OK')").last().click();
    }

    /** The "Change Status" dialog's Confirm button can still be mid-animation right after it opens. */
    private void confirmChangeStatus() {
        confirmButton.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        page.waitForTimeout(500);
        confirmButton.click();
    }

    private void selectNgOption(String formControlName, String optionText) {
        page.locator("ng-select[formcontrolname='" + formControlName + "']").click();
        page.locator(".ng-option:has-text('" + optionText + "')").first().click();
    }

    private void selectNgOptionBySearch(String formControlName, String searchText) {
        page.locator("ng-select[formcontrolname='" + formControlName + "'] input").fill(searchText);
        page.locator(".ng-option:has-text('" + searchText + "')").first().click();
    }

    /** Opens the dropdown and picks its first real option, waiting out a transient "No items found". */
    private void selectFirstRealNgOption(String formControlName) {
        page.locator("ng-select[formcontrolname='" + formControlName + "']").click();
        Locator firstOption = page.locator(".ng-option").first();
        for (int attempt = 0; attempt < 20 && firstOption.innerText().contains("No items found"); attempt++) {
            page.waitForTimeout(500);
        }
        firstOption.click();
    }

    public boolean isSuccessToastVisible(String message) {
        try {
            page.getByText(message).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }

    /** Activate shows no success toast (unlike Approve), so check the status label directly instead. */
    public boolean isActiveStatusVisible() {
        try {
            page.getByText("[Active]").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }
}
