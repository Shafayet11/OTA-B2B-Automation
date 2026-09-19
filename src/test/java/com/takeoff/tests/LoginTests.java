package com.takeoff.tests;
import com.takeoff.base.BaseTest;
import com.takeoff.config.ConfigManager;
import com.takeoff.pages.LoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

@Epic("B2B OTA Portal")
@Feature("Authentication")
public class LoginTests extends BaseTest {


    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("Logging in with valid password ")
    public void validCredentialsLogInSuccessfully() {
        LoginPage loginPage = new LoginPage(page);
        loginPage.login(ConfigManager.email(), ConfigManager.password());

        Assert.assertFalse(loginPage.isDisplayed(), "Login should succeed with valid credentials");
    }
}
