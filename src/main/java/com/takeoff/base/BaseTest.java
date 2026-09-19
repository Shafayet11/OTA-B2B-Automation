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
        page = PlaywrightFactory.initBrowser();
        page.navigate(ConfigManager.b2bUrl());
        new HomePage(page).dismissPromoDialogIfPresent();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        PlaywrightFactory.closeBrowser();
    }
}
