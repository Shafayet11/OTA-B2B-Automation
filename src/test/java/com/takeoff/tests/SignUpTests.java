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

import java.nio.file.Path;

/**
 * Covers "Register" (route {@code /signup}, reached via the homepage's
 * "Become a Takeoff Travels" card). Unlike Booking's "Confirm Booking",
 * signing up does not move money directly - it just creates a new agency
 * account/request record - so these tests click Sign Up for real and each
 * run leaves a new account behind (confirmed with the user).
 */
@Epic("B2B OTA Portal")
@Feature("Register")
public class SignUpTests extends BaseTest {

    private static final Path NID_UPLOAD = Path.of("src/test/resources/attachments/dummy-receipt.png");
    private static final String PASSWORD = "TestPass123!";

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
        registerPage.fillRequired(uniqueUsername(), uniqueEmail(), uniquePhone(), "123 Test Street, Dhaka", PASSWORD, null);
        registerPage.agreeToTerms();
        Assert.assertTrue(registerPage.isSubmitEnabled(), "Sign Up should be enabled once every required field is filled and Terms are agreed to");

        registerPage.signUp();

        Assert.assertTrue(registerPage.isSuccessToastVisible(), "Expected a success confirmation after registering a new agency");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("A visitor can register a new agency including the optional NID number and NID upload.")
    public void visitorCanRegisterWithOptionalNidFields() {
        RegisterPage registerPage = new RegisterPage(page);
        registerPage.open();
        registerPage.fillRequired(uniqueUsername(), uniqueEmail(), uniquePhone(), "123 Test Street, Dhaka", PASSWORD, "1234567890123");
        registerPage.attachNid(NID_UPLOAD);
        registerPage.agreeToTerms();

        registerPage.signUp();

        Assert.assertTrue(registerPage.isSuccessToastVisible(), "Expected a success confirmation after registering with NID details");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("Sign Up stays disabled and shows an inline error when Confirm Password doesn't match Password.")
    public void mismatchedConfirmPasswordBlocksSubmit() {
        RegisterPage registerPage = new RegisterPage(page);
        registerPage.open();
        registerPage.fillRequired(uniqueUsername(), uniqueEmail(), uniquePhone(), "123 Test Street, Dhaka", PASSWORD, null);
        registerPage.fillConfirmPassword("Mismatch123!");
        page.keyboard().press("Tab"); // the mismatch error only renders once the field blurs

        Assert.assertTrue(registerPage.isPasswordMismatchErrorVisible(), "Expected a 'Passwords do not match' error");
        Assert.assertFalse(registerPage.isSubmitEnabled(), "Sign Up should stay disabled while passwords don't match");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("Registering again with an email already in use shows a duplicate-email error instead of succeeding.")
    public void duplicateEmailIsRejected() {
        String email = uniqueEmail();
        String address = "123 Test Street, Dhaka";

        RegisterPage registerPage = new RegisterPage(page);
        registerPage.open();
        registerPage.fillRequired(uniqueUsername(), email, uniquePhone(), address, PASSWORD, null);
        registerPage.agreeToTerms();
        registerPage.signUp();
        Assert.assertTrue(registerPage.isSuccessToastVisible(), "Setup: first registration with this email should succeed");

        page.navigate(com.takeoff.config.ConfigManager.b2bUrl());
        new com.takeoff.pages.HomePage(page).dismissPromoDialogIfPresent();
        registerPage.open();
        registerPage.fillRequired(uniqueUsername(), email, uniquePhone(), address, PASSWORD, null);
        registerPage.agreeToTerms();
        registerPage.signUp();

        Assert.assertTrue(registerPage.isErrorToastVisible("This email already exists."), "Expected a duplicate-email error on the second registration");
    }
}
