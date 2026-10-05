package com.takeoff.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * "Reports" ({@code /reports}), reached via the sidebar. Two tabs sharing one
 * page: "Sales Report" (every issued ticket - Date-Time, Booking ID, PNR,
 * Ticket Number, Route, Carrier Name, Passenger Name, Passenger Type, Base
 * Fare, Tax, AIT, Discount, Total Price) and "Ledger Report" (every balance
 * movement on the agent's account - Date-Time, Invoice Number, Booking ID,
 * PNR, Ticket-Number, Description, Type, Debit, Credit, Running Balance,
 * Created By, Invoice). Both default to a date range covering today, so a
 * just-created row is visible without changing the filter.
 */
public class ReportsPage extends BasePage {

    private static final int SALES_COLUMN_BOOKING_ID = 1;
    private static final int SALES_COLUMN_TICKET_NUMBER = 3;
    private static final int SALES_COLUMN_TOTAL_PRICE = 12;

    private static final int LEDGER_COLUMN_BOOKING_ID = 2;
    private static final int LEDGER_COLUMN_DESCRIPTION = 5;
    private static final int LEDGER_COLUMN_DEBIT = 7;
    private static final int LEDGER_COLUMN_CREDIT = 8;

    private final Locator salesReportTab;
    private final Locator ledgerReportTab;

    public ReportsPage(Page page) {
        super(page);
        this.salesReportTab = page.locator("text=Sales Report").first();
        this.ledgerReportTab = page.locator("text=Ledger Report").first();
    }

    /** Navigates here from the sidebar (present on every authenticated page). Lands on the "Sales Report" tab. */
    public void open() {
        page.getByText("Reports", new Page.GetByTextOptions().setExact(true)).first().click();
        page.waitForURL("**/reports", new Page.WaitForURLOptions().setTimeout(15000));
    }

    public void openSalesReport() {
        salesReportTab.click();
    }

    public void openLedgerReport() {
        ledgerReportTab.click();
    }

    /**
     * The first row is visible immediately as an empty/loading skeleton while
     * the table's data finishes fetching (same race as Group Fare's Booking
     * List - see {@link GroupFarePage}), so poll a cell that's non-empty once
     * data arrives rather than reading immediately.
     */
    private String firstRowCell(int columnIndex) {
        Locator firstRow = page.locator("tbody tr").first();
        firstRow.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        Locator firstCell = firstRow.locator("td").first();

        long deadline = System.currentTimeMillis() + 15000;
        while (firstCell.innerText().trim().isEmpty() && System.currentTimeMillis() < deadline) {
            page.waitForTimeout(300);
        }

        return firstRow.locator("td").nth(columnIndex).innerText().trim();
    }

    public int rowCount() {
        return page.locator("tbody tr").count();
    }

    public String latestSalesBookingId() {
        return firstRowCell(SALES_COLUMN_BOOKING_ID);
    }

    public String latestSalesTicketNumber() {
        return firstRowCell(SALES_COLUMN_TICKET_NUMBER);
    }

    public String latestSalesTotalPrice() {
        return firstRowCell(SALES_COLUMN_TOTAL_PRICE);
    }

    public String latestLedgerBookingId() {
        return firstRowCell(LEDGER_COLUMN_BOOKING_ID);
    }

    public String latestLedgerDescription() {
        return firstRowCell(LEDGER_COLUMN_DESCRIPTION);
    }

    public String latestLedgerDebit() {
        return firstRowCell(LEDGER_COLUMN_DEBIT);
    }

    public String latestLedgerCredit() {
        return firstRowCell(LEDGER_COLUMN_CREDIT);
    }
}
