package com.takeoff.tests;

import com.takeoff.base.BaseTest;
import com.takeoff.pages.RegisterPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers "Register" (route {@code /signup}, reached via the homepage's
 * "Become a Takeoff Travels" card). Unlike Booking's "Confirm Booking",
 * signing up does not move money directly - it just creates a new agency
 * account/request record - so this test clicks Sign Up for real and each
 * run leaves a new account behind (confirmed with the user).
 */
@Epic("B2B OTA Portal")
@Feature("Register")
public class SignUpTests extends BaseTest {

    private String uniqueUsername() {
        return "AutoTestAgency" + System.currentTimeMillis();
    }

    private String uniqueEmail() {
        return "autotest" + System.currentTimeMillis() + "@example.com";
    }

    /** Bangladeshi mobile format the form expects: 10 digits starting with 1, no leading 0. */
    private String uniquePhone() {
        return "1" + (700000000 + (System.currentTimeMillis() % 100000000L));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("A visitor can register a new agency with only the required fields (NID is optional).")
    public void visitorCanRegisterWithRequiredFieldsOnly() {
        RegisterPage registerPage = new RegisterPage(page);
        registerPage.open();
        registerPage.fillRequired(uniqueUsername(), uniqueEmail(), uniquePhone(), "123 Test Street, Dhaka", "TestPass123!");
        registerPage.agreeToTerms();
        Assert.assertTrue(registerPage.isSubmitEnabled(), "Sign Up should be enabled once every required field is filled and Terms are agreed to");

        registerPage.signUp();

        Assert.assertTrue(registerPage.isSuccessToastVisible(), "Expected a success confirmation after registering a new agency");
    }
}
