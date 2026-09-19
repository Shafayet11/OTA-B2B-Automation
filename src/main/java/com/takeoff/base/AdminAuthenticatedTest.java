package com.takeoff.base;

import com.takeoff.config.ConfigManager;
import com.takeoff.pages.AdminLoginPage;
import org.testng.annotations.BeforeMethod;

/** Base for Admin portal tests that need to start already logged in. */
public abstract class AdminAuthenticatedTest extends AdminBaseTest {

    @BeforeMethod(dependsOnMethods = "setUp")
    public void logIn() {
        new AdminLoginPage(page).login(ConfigManager.adminEmail(), ConfigManager.adminPassword());
    }
}
