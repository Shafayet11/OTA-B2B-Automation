package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * The B2B login form lives directly on the homepage ("/"), inside a card
 * titled "Already Using Takeoff Travel?". Locators confirmed against the
 * live site.
 */
public class LoginPage extends BasePage {

    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator errorMessage;

    public LoginPage(Page page) {
        super(page);
        this.emailInput = page.locator("#email");
        this.passwordInput = page.locator("#password");
        this.loginButton = page.locator("button[type='submit']");
        this.errorMessage = page.locator("p.text-destructive");
    }

    public void login(String email, String password) {
        emailInput.fill(email);
        passwordInput.fill(password);
        loginButton.click();
    }

    /** Login failure re-renders the page asynchronously, so wait for the message rather than checking immediately. */
    public boolean isDisplayed() {
        try {
            errorMessage.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }

    public String errorText() {
        return errorMessage.textContent();
    }
}
