package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public abstract class BasePage {

    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    public String currentUrl() {
        return page.url();
    }

    public String title() {
        return page.title();
    }

    /**
     * Shared shadcn/radix form pattern across this app's pages: a labeled
     * field renders as a control (input/button) plus a label, both direct
     * children of a "relative" wrapper div - the label sits after the
     * control on some forms (a floating {@code <span>}) and before it on
     * others (a plain {@code <label>}). Scoping to that wrapper is how an
     * individual field gets identified, since most controls share generic
     * placeholders or no name attribute at all.
     */
    protected Locator fieldByLabel(String label) {
        return page.locator(
                "div.relative:has(> span:has-text('" + label + "')), " +
                        "div.relative:has(> label:has-text('" + label + "'))");
    }

    /** Opens a shadcn/radix Select trigger (role=combobox) and picks the option with this exact text. */
    protected void selectDropdown(String label, String optionText) {
        fieldByLabel(label).locator("button[role=combobox]").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(optionText).setExact(true)).click();
    }

    /**
     * Opens a date-picker popover and picks a day. If the target date is
     * already in the calendar's default view (e.g. today, for a "deposit
     * date" field), clicks it directly - some forms tie a short-lived value
     * to the moment the popover opens (the Bkash/Nagad Payment Request forms
     * reject submission with "gateway service charge amount has been
     * modified" once too much time has passed navigating the calendar), so
     * skipping unnecessary navigation matters there. Otherwise falls back to
     * the Year/Month select dropdowns (not prev/next paging, which would be
     * impractical for a decades-old date) - Year first, since the Month
     * dropdown's enabled/disabled options are computed against whichever
     * year is currently showing.
     */
    protected void pickDate(Locator trigger, LocalDate date) {
        trigger.click();
        Locator dialog = page.locator("[role=dialog]").last();
        Locator dayButton = dialog.locator("button[data-day='" + date.getMonthValue() + "/" + date.getDayOfMonth() + "/" + date.getYear() + "']");
        try {
            dayButton.click(new Locator.ClickOptions().setTimeout(1500));
            return;
        } catch (PlaywrightException notInDefaultView) {
            // Falls through to explicit Year/Month navigation below.
        }
        dialog.locator("button[aria-label='Choose the Year']").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(String.valueOf(date.getYear())).setExact(true)).click();
        dialog.locator("button[aria-label='Choose the Month']").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions()
                .setName(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)).setExact(true)).click();
        dayButton.click();
    }
}
