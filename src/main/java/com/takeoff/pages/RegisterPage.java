package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * "Register" (the homepage's "Become a Takeoff Travels" card, route
 * {@code /signup}) - creates a new agency account. Submitting does not move
 * money by itself, it just creates an account/request record, so tests may
 * click {@link #signUp()} for real; unlike Booking's "Confirm Booking" this
 * is not a direct financial transaction (confirmed with the user).
 *
 * <p>Only the happy path (required fields, no NID) is covered - Username,
 * Email, Mobile Number, Address, Password and Confirm Password are required
 * (submit stays disabled until they're filled, passwords match, and the
 * "agree to Terms" checkbox is checked).
 */
public class RegisterPage extends BasePage {

    private final Locator usernameInput;
    private final Locator emailInput;
    private final Locator phoneInput;
    private final Locator addressInput;
    private final Locator passwordInput;
    private final Locator confirmPasswordInput;
    private final Locator agreeCheckbox;
    private final Locator submitButton;
    private final Locator successToast;

    public RegisterPage(Page page) {
        super(page);
        this.usernameInput = page.locator("#userName");
        this.emailInput = page.locator("#email");
        this.phoneInput = page.locator("#phoneNo");
        this.addressInput = page.locator("#address");
        this.passwordInput = page.locator("#password");
        this.confirmPasswordInput = page.locator("#confirmPassword");
        this.agreeCheckbox = page.locator("#agree");
        this.submitButton = page.locator("button[type='submit']");
        this.successToast = page.getByText("Registration Successful");
    }

    /** Navigates here via the "Register" link on the homepage login card. */
    public void open() {
        page.locator("a[href='/signup']").first().click();
        page.waitForURL("**/signup", new Page.WaitForURLOptions().setTimeout(15000));
    }

    public void fillRequired(String username, String email, String phone, String address, String password) {
        usernameInput.fill(username);
        emailInput.fill(email);
        phoneInput.fill(phone);
        addressInput.fill(address);
        passwordInput.fill(password);
        confirmPasswordInput.fill(password);
    }

    public void agreeToTerms() {
        agreeCheckbox.click();
    }

    public boolean isSubmitEnabled() {
        return !submitButton.isDisabled();
    }

    public void signUp() {
        submitButton.click();
    }

    /** Redirects back to the homepage on success, so wait rather than checking immediately. */
    public boolean isSuccessToastVisible() {
        try {
            successToast.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }
}
