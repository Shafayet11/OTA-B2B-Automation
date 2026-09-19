package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * The Admin portal's login form ("/"), an Angular app - a different stack
 * from the B2B agent portal's shadcn/React form (see LoginPage), so fields
 * are located by {@code formcontrolname} rather than {@code id}.
 *
 * <p>A successful login can land on an "Authenticator Setup" (TOTP QR code)
 * screen instead of the dashboard. That screen only occupies the main content
 * pane, not a blocking overlay - the sidebar underneath is still fully
 * interactive, so navigation (see {@link TopupApprovalPage}) works right
 * through it without completing 2FA enrollment.
 */
public class AdminLoginPage extends BasePage {

    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator loginButton;

    public AdminLoginPage(Page page) {
        super(page);
        this.emailInput = page.locator("input[formcontrolname='email']");
        this.passwordInput = page.locator("input[formcontrolname='password']");
        this.loginButton = page.locator("button:has-text('Login')");
    }

    public void login(String email, String password) {
        emailInput.fill(email);
        passwordInput.fill(password);
        loginButton.click();
    }
}
