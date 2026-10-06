package com.takeoff.tests;

import com.takeoff.base.AuthenticatedTest;
import com.takeoff.pages.BookingPage;
import com.takeoff.pages.RefundPage;
import com.takeoff.pages.SearchPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;

/**
 * Covers the Refund module: after a ticket is issued from the B2B agent
 * portal, a "Refund" button appears at the bottom of its ticket copy.
 * Clicking it submits a refund request; the resulting quotation is either
 * generated automatically or has to be created by an admin via the Admin
 * portal's "Refund Request" module - confirmed with the user as not
 * predictable in advance. Every quotation observed during exploration came
 * back automatic, so that's the only path covered here; the manual
 * (admin-created) quotation path is unexplored.
 *
 * <p>Unlike {@link BookingTests}, which deliberately never completes a
 * booking, testing Refund needs an actually-issued ticket, so every test here
 * really searches, books, and issues a fresh ticket with Full Payment before
 * touching Refund (confirmed with the user - see {@link RefundPage} javadoc
 * for why issuance is driven via polling rather than waiting on the page).
 *
 * <p>Both accepting and rejecting a quotation are real actions here. Accept
 * returns the ticket to the airline against its fare rules and credits the
 * refundable amount back. Reject doesn't move money and leaves the ticket
 * Eligible for a fresh request, so {@link #agentCanRejectARefundQuotation()}
 * finishes by re-requesting and accepting on the same ticket to recover that
 * value, rather than leaving a fully-paid, never-refunded ticket behind every
 * run.
 *
 * <p>Every run of this file is a real net loss of the airline penalty plus
 * service charge (observed ~BDT 13,000 on a ~36,000 BDT fare) - issuing and
 * fully refunding a real ticket isn't free, even on the happy path.
 */
@Epic("B2B OTA Portal")
@Feature("Refund")
public class RefundTests extends AuthenticatedTest {

    @Override
    protected double slowMoMs() {
        return 500;
    }

    private String uniqueSuffix() {
        return String.format("%07d", System.currentTimeMillis() % 10000000);
    }

    /**
     * The detailed "Provide Traveller Details" step rejects digits in the
     * name fields ("only letters and single spaces are allowed") - so the
     * last name needs a unique suffix made of letters, not the digit suffix
     * used elsewhere (passport/email/phone have no such restriction).
     */
    private String uniqueAlphaSuffix() {
        long n = System.currentTimeMillis() % 456976; // 26^4
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append((char) ('A' + (int) (n % 26)));
            n /= 26;
        }
        return sb.toString();
    }

    /** Searches, books, and issues a real ticket with Full Payment, returning its booking reference (utid). */
    private String issueFreshTicket(RefundPage refundPage) {
        String suffix = uniqueSuffix();
        String lastName = "AutoTest" + uniqueAlphaSuffix();

        // Filtered to US-Bangla (BS) specifically - which airlines serve DAC-DXB
        // isn't stable day to day, and an Emirates NDC fare showed up
        // non-refundable with a different booking flow entirely (confirmed by
        // exploration), which would silently break everything downstream.
        SearchPage searchPage = new SearchPage(page);
        searchPage.searchOneWayFlight("Dhaka", "DAC", "Dubai", "DXB", LocalDate.now().plusMonths(1), "BS");
        searchPage.selectResult(0);

        BookingPage bookingPage = new BookingPage(page);
        bookingPage.fillRequiredTravelerDetails(
                "Mr", "Refund", lastName, LocalDate.of(1990, 5, 15),
                "RF" + suffix, LocalDate.now().minusYears(2), LocalDate.now().plusYears(3),
                "refund.autotest." + suffix + "@example.com", "171" + suffix);
        bookingPage.confirmBooking();

        refundPage.agreeAndConfirmReview();
        String reference = refundPage.currentBookingReference();
        refundPage.agreeAndIssueTicket();
        refundPage.waitForTicketIssued(reference);
        return reference;
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("An agent can issue a ticket, request a refund via its ticket copy's Refund button, and accept the resulting quotation.")
    public void agentCanAcceptARefundQuotation() {
        RefundPage refundPage = new RefundPage(page);
        String reference = issueFreshTicket(refundPage);

        refundPage.openTicketView(reference);
        refundPage.clickRefund();
        refundPage.selectAllForRefund();
        refundPage.fillAgentRemarks("Automated refund test - accept path.");
        refundPage.getQuotation();
        Assert.assertTrue(refundPage.isQuotationSummaryVisible(), "Expected a refund quotation to be generated");

        refundPage.acceptQuotation();
        Assert.assertTrue(page.url().contains("refund-list"), "Expected to land back on the refund list after accepting the quotation");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Description("An agent can reject a refund quotation - the ticket becomes Eligible again instead of staying stuck, and a fresh request can recover it.")
    public void agentCanRejectARefundQuotation() {
        RefundPage refundPage = new RefundPage(page);
        String reference = issueFreshTicket(refundPage);

        refundPage.openTicketView(reference);
        refundPage.clickRefund();
        refundPage.selectAllForRefund();
        refundPage.getQuotation();
        refundPage.rejectQuotation();

        refundPage.openRefundRequestForm(reference);
        Assert.assertTrue(refundPage.isEligible(), "Expected the ticket to be Eligible again after rejecting its quotation");

        // Recover the refund value rather than leaving a fully-paid, never-refunded ticket behind.
        refundPage.selectAllForRefund();
        refundPage.getQuotation();
        refundPage.acceptQuotation();
    }
}
