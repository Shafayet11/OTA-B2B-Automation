package com.takeoff.tests;

import com.takeoff.base.AuthenticatedTest;
import com.takeoff.config.ConfigManager;
import com.takeoff.pages.AdminLoginPage;
import com.takeoff.pages.ReportsPage;
import com.takeoff.pages.TopupRequestPage;
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
 * Covers both sides of the top-up flow: the B2B agent submitting a deposit/
 * top-up request on "Payment Request" ({@code /topup-request}), and an admin
 * reviewing it on the Admin portal (Balance Management &gt; Topup &gt; View
 * Request). Merged into one file/page object ({@link TopupRequestPage}) since
 * every admin-review test also has to submit a fresh request first.
 *
 * <p>Unlike Booking's "Confirm Booking", submitting a request does not move
 * money by itself - it creates a request for someone else to review - so
 * these tests click Submit for real and each run leaves a new request record
 * behind.
 *
 * <p>Cash, Bkash and Nagad work end-to-end (the "Gateway service charge
 * amount has been modified" bug that used to block Bkash/Nagad is fixed).
 * Cheque, Bank Deposit and Bank Transfer are still blocked by an empty
 * "Deposited In" dropdown - an app/data bug, not a test issue. Those 3 submit
 * tests are disabled with a javadoc note on each, and there's nothing to
 * review for them admin-side either - their fill methods on
 * {@link TopupRequestPage} are fully implemented and ready to use once the
 * underlying bug is fixed.
 *
 * <p>Approving credits the agent's account balance for real, so - same
 * convention as Booking's "Confirm Booking" - the review tests fill the
 * Admin Reference/Remarks fields and assert Approve is enabled, but never
 * click it (see {@link TopupRequestPage} javadoc). Reject and Pending only
 * change the request's status rather than moving money, so those tests
 * actually click through.
 */
@Epic("B2B OTA Portal")
@Feature("Topup Request")
public class TopupRequestTests extends AuthenticatedTest {

    private static final Path RECEIPT = Path.of("src/test/resources/attachments/dummy-receipt.png");

    /** Slowed down from config.properties' default so the admin-review flow is easy to watch/verify visually. */
    @Override
    protected double slowMoMs() {
        return 500;
    }

    private String uniqueReference() {
        return "REF-AUTOTEST-" + System.currentTimeMillis();
    }

    /** bKash/Nagad reject anything that doesn't look like a real 10-char alphanumeric transaction ID. */
    private String randomTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    /**
     * Switches the same browser tab from the agent portal to the Admin
     * portal, logs in as admin, and opens the most recent "Requested" row of
     * the given deposit type submitted by this test's agent account.
     */
    private TopupRequestPage openLatestRequestAsAdmin(String depositType) {
        // The admin portal's request list has a brief backend propagation delay after
        // submission - without this wait, openLatestRequest() can race and grab a
        // stale leftover "Requested" row instead of the one just submitted.
        page.waitForTimeout(5000);
        page.navigate(ConfigManager.adminUrl());
        new AdminLoginPage(page).login(ConfigManager.adminEmail(), ConfigManager.adminPassword());

        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.openAdminRequestList();
        topupRequestPage.openLatestRequest(depositType, ConfigManager.email());
        return topupRequestPage;
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Cash deposit request.")
    public void agentCanSubmitCashDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillCash("500", "Automation Test Branch", uniqueReference(), RECEIPT);
        topupRequestPage.submit();

        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Cash deposit request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Bkash deposit request.")
    public void agentCanSubmitBkashDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillBkash("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        topupRequestPage.submit();

        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Bkash deposit request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Nagad deposit request.")
    public void agentCanSubmitNagadDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillNagad("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        topupRequestPage.submit();

        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Nagad deposit request");
    }

    /**
     * Disabled: the "Deposited In" dropdown this type requires is empty in
     * this environment (confirmed via the underlying native {@code <select>}
     * having zero {@code <option>}s across repeated checks, despite "Partner
     * Bank Details" listing 6 valid accounts) - an app/data bug, not a test
     * issue. Flip {@code enabled} once it's fixed.
     */
    @Test(enabled = false)
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Cheque deposit request. BLOCKED: see method javadoc.")
    public void agentCanSubmitChequeDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillCheque("500", LocalDate.now(), "CHQ-" + System.currentTimeMillis(),
                "Technonext Bank", "Dutch Bangla Bank Limited", uniqueReference(), "0", RECEIPT);
        topupRequestPage.submit();

        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Cheque deposit request");
    }

    /** Disabled: same "Deposited In" blocker as {@link #agentCanSubmitChequeDepositRequest}. */
    @Test(enabled = false)
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Bank Deposit request. BLOCKED: see method javadoc.")
    public void agentCanSubmitBankDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillBankDeposit("500", LocalDate.now(), "Dutch Bangla Bank Limited", uniqueReference(), "0", RECEIPT);
        topupRequestPage.submit();

        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Bank Deposit request");
    }

    /** Disabled: same "Deposited In" blocker as {@link #agentCanSubmitChequeDepositRequest} (also needs "Deposited From"). */
    @Test(enabled = false)
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Bank Transfer request. BLOCKED: see method javadoc.")
    public void agentCanSubmitBankTransferRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillBankTransfer("500", LocalDate.now(), "Dutch Bangla Bank Limited",
                "Technonext Test", uniqueReference(), "0", RECEIPT);
        topupRequestPage.submit();

        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Bank Transfer request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can open a submitted Cash top-up request and Approve is available for it.")
    public void adminCanReviewCashDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillCash("500", "Automation Test Branch", uniqueReference(), RECEIPT);
        topupRequestPage.submit();
        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Cash deposit request");

        TopupRequestPage adminView = openLatestRequestAsAdmin("Cash");
        adminView.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Reviewed by automated test.");

        Assert.assertTrue(adminView.isApproveButtonEnabled(), "Expected Approve to be enabled for a fully-filled Cash top-up request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can open a submitted Bkash top-up request and Approve is available for it.")
    public void adminCanReviewBkashDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillBkash("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        topupRequestPage.submit();
        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Bkash deposit request");

        TopupRequestPage adminView = openLatestRequestAsAdmin("BKash");
        adminView.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Reviewed by automated test.");

        Assert.assertTrue(adminView.isApproveButtonEnabled(), "Expected Approve to be enabled for a fully-filled Bkash top-up request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can open a submitted Nagad top-up request and Approve is available for it.")
    public void adminCanReviewNagadDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillNagad("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        topupRequestPage.submit();
        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Nagad deposit request");

        TopupRequestPage adminView = openLatestRequestAsAdmin("Nagad");
        adminView.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Reviewed by automated test.");

        Assert.assertTrue(adminView.isApproveButtonEnabled(), "Expected Approve to be enabled for a fully-filled Nagad top-up request");
    }

    // Reject and Pending only change the request's status - unlike Approve, they don't move
    // money (confirmed with the team) - so these tests actually click through, unlike the
    // Approve tests above.

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can reject a submitted Cash top-up request.")
    public void adminCanRejectCashDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillCash("500", "Automation Test Branch", uniqueReference(), RECEIPT);
        topupRequestPage.submit();
        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Cash deposit request");

        TopupRequestPage adminView = openLatestRequestAsAdmin("Cash");
        adminView.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Rejected by automated test.");
        adminView.reject(ConfigManager.adminPin());

        Assert.assertTrue(adminView.isStatusChangeSuccessToastVisible(), "Expected a success confirmation after rejecting a Cash top-up request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can reject a submitted Bkash top-up request.")
    public void adminCanRejectBkashDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillBkash("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        topupRequestPage.submit();
        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Bkash deposit request");

        TopupRequestPage adminView = openLatestRequestAsAdmin("BKash");
        adminView.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Rejected by automated test.");
        adminView.reject(ConfigManager.adminPin());

        Assert.assertTrue(adminView.isStatusChangeSuccessToastVisible(), "Expected a success confirmation after rejecting a Bkash top-up request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can reject a submitted Nagad top-up request.")
    public void adminCanRejectNagadDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillNagad("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        topupRequestPage.submit();
        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Nagad deposit request");

        TopupRequestPage adminView = openLatestRequestAsAdmin("Nagad");
        adminView.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Rejected by automated test.");
        adminView.reject(ConfigManager.adminPin());

        Assert.assertTrue(adminView.isStatusChangeSuccessToastVisible(), "Expected a success confirmation after rejecting a Nagad top-up request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can put a submitted Cash top-up request on hold (Pending).")
    public void adminCanSetPendingCashDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillCash("500", "Automation Test Branch", uniqueReference(), RECEIPT);
        topupRequestPage.submit();
        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Cash deposit request");

        TopupRequestPage adminView = openLatestRequestAsAdmin("Cash");
        adminView.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Held by automated test.");
        adminView.setPending(ConfigManager.adminPin());

        Assert.assertTrue(adminView.isStatusChangeSuccessToastVisible(), "Expected a success confirmation after putting a Cash top-up request on hold");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can put a submitted Bkash top-up request on hold (Pending).")
    public void adminCanSetPendingBkashDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillBkash("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        topupRequestPage.submit();
        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Bkash deposit request");

        TopupRequestPage adminView = openLatestRequestAsAdmin("BKash");
        adminView.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Held by automated test.");
        adminView.setPending(ConfigManager.adminPin());

        Assert.assertTrue(adminView.isStatusChangeSuccessToastVisible(), "Expected a success confirmation after putting a Bkash top-up request on hold");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An admin can put a submitted Nagad top-up request on hold (Pending).")
    public void adminCanSetPendingNagadDepositRequest() {
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillNagad("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        topupRequestPage.submit();
        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Nagad deposit request");

        TopupRequestPage adminView = openLatestRequestAsAdmin("Nagad");
        adminView.fillApprovalDetails("ADMIN-REF-AUTOTEST", "Held by automated test.");
        adminView.setPending(ConfigManager.adminPin());

        Assert.assertTrue(adminView.isStatusChangeSuccessToastVisible(), "Expected a success confirmation after putting a Nagad top-up request on hold");
    }

    /**
     * Unlike every other admin-review test above, this one actually clicks
     * Approve - by explicit user instruction, so the resulting credit can be
     * verified on the agent's Ledger Report. Approving credits the agent's
     * account balance for real on every run (same category as Booking's
     * "Confirm Booking" - see {@link TopupRequestPage} javadoc).
     */
    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("Approving a Cash top-up request credits the agent's account, and that credit appears on the agent's Ledger Report.")
    public void adminApprovingCashDepositRequestCreditsAgentLedger() {
        String reference = uniqueReference();
        TopupRequestPage topupRequestPage = new TopupRequestPage(page);
        topupRequestPage.open();
        topupRequestPage.fillCash("500", "Automation Test Branch", reference, RECEIPT);
        topupRequestPage.submit();
        Assert.assertTrue(topupRequestPage.isSubmitSuccessToastVisible(), "Expected a success confirmation after submitting a Cash deposit request");

        TopupRequestPage adminView = openLatestRequestAsAdmin("Cash");
        // Approve posts a real ledger transaction, so (unlike Reject/Pending) the backend
        // rejects a reused Admin Reference with "Referance Already Exist" - must be unique per run.
        adminView.fillApprovalDetails("ADMIN-REF-AUTOTEST-" + System.currentTimeMillis(), "Approved by automated test.");
        adminView.approve(ConfigManager.adminPin());
        Assert.assertTrue(adminView.isStatusChangeSuccessToastVisible(), "Expected a success confirmation after approving a Cash top-up request");

        // Back to the agent portal - the agent session cookie is still valid, no re-login needed.
        page.navigate(ConfigManager.b2bUrl());
        ReportsPage reportsPage = new ReportsPage(page);
        reportsPage.open();
        reportsPage.openLedgerReport();

        Assert.assertEquals(reportsPage.latestLedgerCredit(), "500.00", "Expected the approved Cash top-up to appear as a 500.00 credit on the Ledger Report");
    }
}
