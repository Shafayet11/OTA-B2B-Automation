package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;

/**
 * The homepage ("/") also hosts the B2B login card (see LoginPage) and, on
 * some loads, a promotional dialog that overlays the whole page and blocks
 * every click until dismissed.
 */
public class HomePage extends BasePage {

    private final Locator closeDialogButton;

    public HomePage(Page page) {
        super(page);
        this.closeDialogButton = page.locator("button:has-text('Close')");
    }

    /** No-op if the promo dialog isn't showing. */
    public void dismissPromoDialogIfPresent() {
        try {
            closeDialogButton.first().click(new Locator.ClickOptions().setTimeout(3000));
        } catch (PlaywrightException ignored) {
        }
    }
}
