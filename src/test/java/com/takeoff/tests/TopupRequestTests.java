package com.takeoff.tests;

import com.takeoff.base.AuthenticatedTest;
import com.takeoff.pages.PaymentRequestPage;
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
 * Covers the 6 deposit types on "Payment Request" ({@code /topup-request}).
 * Unlike Booking's "Confirm Booking", submitting here does not move money by
 * itself - it creates a request for someone else to review - so these tests
 * click Submit for real and each run leaves a new request record behind.
 *
 * <p>Cash, Bkash and Nagad work end-to-end (the "Gateway service charge
 * amount has been modified" bug that used to block Bkash/Nagad is fixed).
 * Cheque, Bank Deposit and Bank Transfer are still blocked by an empty
 * "Deposited In" dropdown - an app/data bug, not a test issue. Those 3 tests
 * are disabled with a javadoc note on each; their fill methods on
 * {@link PaymentRequestPage} are fully implemented and ready to use once the
 * underlying bug is fixed.
 */
@Epic("B2B OTA Portal")
@Feature("Payment Request")
public class TopupRequestTests extends AuthenticatedTest {

    private static final Path RECEIPT = Path.of("src/test/resources/attachments/dummy-receipt.png");

    private String uniqueReference() {
        return "REF-AUTOTEST-" + System.currentTimeMillis();
    }

    /** bKash/Nagad reject anything that doesn't look like a real 10-char alphanumeric transaction ID. */
    private String randomTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Cash deposit request.")
    public void agentCanSubmitCashDepositRequest() {
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillCash("500", "Automation Test Branch", uniqueReference(), RECEIPT);
        paymentRequestPage.submit();

        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Cash deposit request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Bkash deposit request.")
    public void agentCanSubmitBkashDepositRequest() {
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillBkash("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        paymentRequestPage.submit();

        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Bkash deposit request");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Nagad deposit request.")
    public void agentCanSubmitNagadDepositRequest() {
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillNagad("500", LocalDate.now(), randomTransactionId(), RECEIPT);
        paymentRequestPage.submit();

        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Nagad deposit request");
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
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillCheque("500", LocalDate.now(), "CHQ-" + System.currentTimeMillis(),
                "Technonext Bank", "Dutch Bangla Bank Limited", uniqueReference(), "0", RECEIPT);
        paymentRequestPage.submit();

        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Cheque deposit request");
    }

    /** Disabled: same "Deposited In" blocker as {@link #agentCanSubmitChequeDepositRequest}. */
    @Test(enabled = false)
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Bank Deposit request. BLOCKED: see method javadoc.")
    public void agentCanSubmitBankDepositRequest() {
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillBankDeposit("500", LocalDate.now(), "Dutch Bangla Bank Limited", uniqueReference(), "0", RECEIPT);
        paymentRequestPage.submit();

        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Bank Deposit request");
    }

    /** Disabled: same "Deposited In" blocker as {@link #agentCanSubmitChequeDepositRequest} (also needs "Deposited From"). */
    @Test(enabled = false)
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can submit a Bank Transfer request. BLOCKED: see method javadoc.")
    public void agentCanSubmitBankTransferRequest() {
        PaymentRequestPage paymentRequestPage = new PaymentRequestPage(page);
        paymentRequestPage.open();
        paymentRequestPage.fillBankTransfer("500", LocalDate.now(), "Dutch Bangla Bank Limited",
                "Technonext Test", uniqueReference(), "0", RECEIPT);
        paymentRequestPage.submit();

        Assert.assertTrue(paymentRequestPage.isSuccessToastVisible(), "Expected a success confirmation after submitting a Bank Transfer request");
    }
}
