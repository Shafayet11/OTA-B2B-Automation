package com.takeoff.base;

import com.takeoff.config.ConfigManager;
import com.takeoff.pages.HomePage;
import com.takeoff.pages.LoginPage;
import org.testng.annotations.BeforeMethod;

/** Base for tests that need to start already logged in (Search, Booking, ...). */
public abstract class AuthenticatedTest extends BaseTest {

    @BeforeMethod(dependsOnMethods = "setUp")
    public void logIn() {
        new LoginPage(page).login(ConfigManager.email(), ConfigManager.password());
        // The promo dialog can re-appear on the post-login page too.
        new HomePage(page).dismissPromoDialogIfPresent();
    }
}
