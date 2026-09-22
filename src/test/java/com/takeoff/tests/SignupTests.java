package com.takeoff.tests;

import com.takeoff.base.BaseTest;
import com.takeoff.config.ConfigManager;
import com.takeoff.pages.AdminLoginPage;
import com.takeoff.pages.AgentApprovalPage;
import com.takeoff.pages.RegisterPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the full agency onboarding flow: a visitor registers via the
 * homepage's "Register" card (route {@code /signup}, see {@link RegisterPage}),
 * then an admin approves the request and activates the new agent (Admin
 * portal &gt; B2B Management &gt; Requested Agent / Approved Agent, see
 * {@link AgentApprovalPage}).
 *
 * <p>None of Sign Up, Approve or Active move money directly - they create or
 * advance an account/request record - so this test clicks through all three
 * for real and leaves a new, activated agent account behind each run
 * (confirmed with the user).
 */
@Epic("B2B OTA Portal")
@Feature("Register")
public class SignupTests extends BaseTest {

    /** Slowed down from config.properties' default so the flow is easy to watch/verify visually. */
    @Override
    protected double slowMoMs() {
        return 500;
    }

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
    @Description("A visitor can register a new agency, and an admin can approve and activate it.")
    public void visitorCanRegisterAndAdminCanApproveAndActivateAgency() {
        String email = uniqueEmail();

        RegisterPage registerPage = new RegisterPage(page);
        registerPage.open();
        registerPage.fillRequired(uniqueUsername(), email, uniquePhone(), "123 Test Street, Dhaka", "TestPass123!");
        registerPage.agreeToTerms();
        Assert.assertTrue(registerPage.isSubmitEnabled(), "Sign Up should be enabled once every required field is filled and Terms are agreed to");

        registerPage.signUp();
        Assert.assertTrue(registerPage.isSuccessToastVisible(), "Expected a success confirmation after registering a new agency");

        page.navigate(ConfigManager.adminUrl());
        new AdminLoginPage(page).login(ConfigManager.adminEmail(), ConfigManager.adminPassword());

        AgentApprovalPage agentApprovalPage = new AgentApprovalPage(page);
        agentApprovalPage.openRequested();
        agentApprovalPage.searchByEmail(email);
        agentApprovalPage.openEdit();
        agentApprovalPage.fillMandatoryFieldsForApproval("1213");
        agentApprovalPage.save();

        agentApprovalPage.searchByEmail(email);
        agentApprovalPage.approve();
        Assert.assertTrue(agentApprovalPage.isSuccessToastVisible("Successfully"), "Expected a success confirmation after approving the new agency");

        agentApprovalPage.openApproved();
        agentApprovalPage.searchByEmail(email);
        agentApprovalPage.activate();
        Assert.assertTrue(agentApprovalPage.isActiveStatusVisible(), "Expected the agent's status to become Active after activating it");
    }
}
