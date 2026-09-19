package com.takeoff.base;

import com.microsoft.playwright.Page;
import com.takeoff.config.ConfigManager;
import com.takeoff.core.PlaywrightFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base for Admin portal tests: fresh browser/page per test method, navigated
 * to the Admin portal's base URL. Separate from {@link BaseTest} since that
 * one always navigates to the B2B agent portal instead.
 */
public abstract class AdminBaseTest {

    protected Page page;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        page = PlaywrightFactory.initBrowser();
        page.navigate(ConfigManager.adminUrl());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        PlaywrightFactory.closeBrowser();
    }
}
