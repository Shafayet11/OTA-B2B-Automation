package com.takeoff.tests;

import com.takeoff.base.BaseTest;
import com.takeoff.config.ConfigManager;
import com.takeoff.pages.AdminLoginPage;
import com.takeoff.pages.HomePage;
import com.takeoff.pages.LoginPage;
import com.takeoff.pages.PaymentRequestPage;
import com.takeoff.pages.TopupApprovalPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Covers an admin reviewing a top-up request from Admin portal &gt; Balance
 * Management &gt; Topup &gt; View Request. Each test submits a fresh request
 * as the B2B agent (see TopupRequestTests), then switches to the Admin
 * portal to review it.
 *
 * <p>Approving credits the agent's account balance for real, so - same
 * convention as Booking's "Confirm Booking" - these tests fill the Admin
 * Reference/Remarks fields and assert Approve is enabled, but never click
 * it (see {@link TopupApprovalPage} javadoc). Reject and Pending only
 * change the request's status rather than moving money, so those tests
 * actually click through.
 *
 * <p>Only covers Cash, Bkash and Nagad - the 3 deposit types that currently
 * submit successfully (see TopupRequestTests javadoc for why Cheque, Bank
 * Deposit and Bank Transfer don't, so there's no request to review for them).
 */
@Epic("B2B OTA Portal")
@Feature("Topup Approval")
public class TopupApprovalTests extends BaseTest {

    private static final Path RECEIPT = Path.of("src/test/resources/attachments/dummy-receipt.png");

    private String uniqueReference() {
        return "REF-AUTOTEST-" + System.currentTimeMillis();
    }

    /** bKash/Nagad reject anything that doesn't look like a real 10-char alphanumeric transaction ID. */
    private String randomTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private void loginAsAgent() {
        new LoginPage(page).login(ConfigManager.email(), ConfigManager.password());
        new HomePage(page).dismissPromoDialogIfPresent();
    }

    private TopupApprovalPage openLatestRequestAsAdmin(String depositType) {
        // The admin portal's request list has a brief backend propagation delay after
        // submission - without this wait, openLatestRequest() can race and grab a
        // stale leftover "Requested" row instead of the one just submitted.
        page.waitForTimeout(5000);
        page.navigate(ConfigManager.adminUrl());
        new AdminLoginPage(page).login(ConfigManager.adminEmail(), ConfigManager.adminPassword());

        TopupApprovalPage topupApprovalPage = new TopupApprovalPage(page);
        topupApprovalPage.open();
        topupApprovalPage.openLatestRequest(depositType, ConfigManager.email());
        return topupApprovalPage;
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can open a submitted Cash top-up request and Approve is available for it.")
    public void adminCanReviewCashDepositRequest() {
        loginAsAgent();
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillCash("500", "Automation Test Branch", uniqueReference(), RECEIPT);
        paymentRequestPage.submit();
        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Cash deposit request");

        TopupApprovalPage topupApprovalPage = openLatestRequestAsAdmin("Cash");
        topupApprovalPage.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Reviewed by automated test.");

        Assert.assertTrue(topupApprovalPage.isApproveButtonEnabled(), "Expected Approve to be enabled for a fully-filled Cash top-up request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can open a submitted Bkash top-up request and Approve is available for it.")
    public void adminCanReviewBkashDepositRequest() {
        loginAsAgent();
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillBkash("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        paymentRequestPage.submit();
        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Bkash deposit request");

        TopupApprovalPage topupApprovalPage = openLatestRequestAsAdmin("BKash");
        topupApprovalPage.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Reviewed by automated test.");

        Assert.assertTrue(topupApprovalPage.isApproveButtonEnabled(), "Expected Approve to be enabled for a fully-filled Bkash top-up request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can open a submitted Nagad top-up request and Approve is available for it.")
    public void adminCanReviewNagadDepositRequest() {
        loginAsAgent();
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillNagad("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        paymentRequestPage.submit();
        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Nagad deposit request");

        TopupApprovalPage topupApprovalPage = openLatestRequestAsAdmin("Nagad");
        topupApprovalPage.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Reviewed by automated test.");

        Assert.assertTrue(topupApprovalPage.isApproveButtonEnabled(), "Expected Approve to be enabled for a fully-filled Nagad top-up request");
    }

    // Reject and Pending only change the request's status - unlike Approve, they don't move
    // money (confirmed with the team) - so these tests actually click through, unlike the
    // Approve tests above.

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can reject a submitted Cash top-up request.")
    public void adminCanRejectCashDepositRequest() {
        loginAsAgent();
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillCash("500", "Automation Test Branch", uniqueReference(), RECEIPT);
        paymentRequestPage.submit();
        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Cash deposit request");

        TopupApprovalPage topupApprovalPage = openLatestRequestAsAdmin("Cash");
        topupApprovalPage.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Rejected by automated test.");
        topupApprovalPage.reject(ConfigManager.adminPin());

        Assert.assertTrue(topupApprovalPage.isSuccessToastVisible(), "Expected a success confirmation after rejecting a Cash top-up request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can reject a submitted Bkash top-up request.")
    public void adminCanRejectBkashDepositRequest() {
        loginAsAgent();
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillBkash("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        paymentRequestPage.submit();
        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Bkash deposit request");

        TopupApprovalPage topupApprovalPage = openLatestRequestAsAdmin("BKash");
        topupApprovalPage.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Rejected by automated test.");
        topupApprovalPage.reject(ConfigManager.adminPin());

        Assert.assertTrue(topupApprovalPage.isSuccessToastVisible(), "Expected a success confirmation after rejecting a Bkash top-up request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can reject a submitted Nagad top-up request.")
    public void adminCanRejectNagadDepositRequest() {
        loginAsAgent();
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillNagad("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        paymentRequestPage.submit();
        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Nagad deposit request");

        TopupApprovalPage topupApprovalPage = openLatestRequestAsAdmin("Nagad");
        topupApprovalPage.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Rejected by automated test.");
        topupApprovalPage.reject(ConfigManager.adminPin());

        Assert.assertTrue(topupApprovalPage.isSuccessToastVisible(), "Expected a success confirmation after rejecting a Nagad top-up request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can put a submitted Cash top-up request on hold (Pending).")
    public void adminCanSetPendingCashDepositRequest() {
        loginAsAgent();
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillCash("500", "Automation Test Branch", uniqueReference(), RECEIPT);
        paymentRequestPage.submit();
        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Cash deposit request");

        TopupApprovalPage topupApprovalPage = openLatestRequestAsAdmin("Cash");
        topupApprovalPage.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Held by automated test.");
        topupApprovalPage.setPending(ConfigManager.adminPin());

        Assert.assertTrue(topupApprovalPage.isSuccessToastVisible(), "Expected a success confirmation after putting a Cash top-up request on hold");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can put a submitted Bkash top-up request on hold (Pending).")
    public void adminCanSetPendingBkashDepositRequest() {
        loginAsAgent();
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillBkash("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        paymentRequestPage.submit();
        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Bkash deposit request");

        TopupApprovalPage topupApprovalPage = openLatestRequestAsAdmin("BKash");
        topupApprovalPage.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Held by automated test.");
        topupApprovalPage.setPending(ConfigManager.adminPin());

        Assert.assertTrue(topupApprovalPage.isSuccessToastVisible(), "Expected a success confirmation after putting a Bkash top-up request on hold");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can put a submitted Nagad top-up request on hold (Pending).")
    public void adminCanSetPendingNagadDepositRequest() {
        loginAsAgent();
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillNagad("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        paymentRequestPage.submit();
        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Nagad deposit request");

        TopupApprovalPage topupApprovalPage = openLatestRequestAsAdmin("Nagad");
        topupApprovalPage.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Held by automated test.");
        topupApprovalPage.setPending(ConfigManager.adminPin());

        Assert.assertTrue(topupApprovalPage.isSuccessToastVisible(), "Expected a success confirmation after putting a Nagad top-up request on hold");
    }
}
