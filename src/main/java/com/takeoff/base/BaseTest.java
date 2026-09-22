package com.takeoff.base;

import com.microsoft.playwright.Page;
import com.takeoff.config.ConfigManager;
import com.takeoff.core.PlaywrightFactory;
import com.takeoff.pages.HomePage;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Every test class extends this to get a fresh browser/page per test method
 * and automatic navigation to the configured base URL.
 */
public abstract class BaseTest {

    protected Page page;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        page = PlaywrightFactory.initBrowser(slowMoMs());
        page.navigate(ConfigManager.b2bUrl());
        new HomePage(page).dismissPromoDialogIfPresent();
    }

    /** Override to slow a specific test class down for visual observation. */
    protected double slowMoMs() {
        return ConfigManager.slowMoMs();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        PlaywrightFactory.closeBrowser();
    }
}
