package com.takeoff.pages;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * "Group Fare" ({@code /group-fares}), reached via the sidebar. Lists
 * discounted group-fare deals under its default "Group Fare" tab (a
 * "Booking List" tab sits alongside it - {@code /group-fares/list} - showing
 * past bookings, filterable by "Booking ID" among other fields). Picking a deal's
 * "Book Now" opens a "Select Passengers" modal (adult/child counts default to
 * 1/0, which is enough for a single-passenger booking; each "Add" stepper
 * disables itself once the deal's remaining seat capacity is used up), then
 * "Add Passenger Info" reaches {@code /group-fares/form} - the same kind of
 * traveler-details step as {@link BookingPage}, ending in a "Confirm Booking"
 * that moves money and books against live inventory. CAUTION: {@link
 * #confirmBooking()} submits a real reservation and charges the agent's
 * account balance every time it's called - unlike {@code
 * BookingPage.confirmBooking()} (never called from tests), the bulk-passenger
 * spreadsheet-upload test in {@code GroupFareTests} calls it deliberately,
 * by explicit user instruction, so that test creates a new live booking on
 * every run.
 *
 * <p>For more than one passenger, the form offers "Fill passengers from a
 * spreadsheet": download a "Template" {@code .xlsx} (a worked one-row example
 * plus hidden VLOOKUP helper columns that translate human-readable
 * dropdown labels into the codes the app expects), fill a row per passenger,
 * and upload it to auto-fill every passenger-details field below at once.
 * {@link #fillPassengerDetailsFromSpreadsheet} drives that flow directly with
 * Apache POI instead of a human editing the file in Excel.
 */
public class GroupFarePage extends BasePage {

    private static final String PASSENGERS_SHEET = "Passengers";
    private static final DateTimeFormatter TEMPLATE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Adult is 12+ years (per the Select Passengers modal); any age comfortably past that boundary is fine here. */
    private static final int SAMPLE_ADULT_AGE_YEARS = 30;
    /** Child is 2-11 years (2 to less than 12); any age comfortably inside that range is fine here. */
    private static final int SAMPLE_CHILD_AGE_YEARS = 8;

    /**
     * The Booking List table's columns, left to right: Booking Date, Booking
     * ID, Trip, Flight Date, Route, PNR, Pax, Total, Status, Actions. A
     * freshly confirmed booking appears as the first row.
     */
    private static final int BOOKING_LIST_COLUMN_ID = 1;
    private static final int BOOKING_LIST_COLUMN_PAX = 6;
    private static final int BOOKING_LIST_COLUMN_STATUS = 8;

    private final Locator bookNowButtons;
    private final Locator addAdultButton;
    private final Locator addChildButton;
    private final Locator adultCountLabel;
    private final Locator childCountLabel;
    private final Locator addPassengerInfoButton;
    private final Locator downloadTemplateLink;
    private final Locator excelUploadInput;
    private final Locator firstNameInput;
    private final Locator agreeTermsCheckbox;
    private final Locator confirmBookingButton;
    private final Locator bookingIdFilterInput;
    private final Locator applyFilterButton;

    private int achievedAdultCount;
    private int achievedChildCount;

    public GroupFarePage(Page page) {
        super(page);
        this.bookNowButtons = page.locator("button:has-text('Book Now')");
        this.addAdultButton = page.locator("button[aria-label='Add one adult']");
        this.addChildButton = page.locator("button[aria-label='Add one child']");
        this.adultCountLabel = page.locator("button[aria-label='Remove one adult'] + span");
        this.childCountLabel = page.locator("button[aria-label='Remove one child'] + span");
        this.addPassengerInfoButton = page.locator("button:has-text('Add Passenger Info')");
        this.downloadTemplateLink = page.locator("a:has-text('Template')");
        this.excelUploadInput = page.locator("input[accept='.xls,.xlsx']");
        this.firstNameInput = page.locator("input[name='passengerInfoes.0.first']");
        this.agreeTermsCheckbox = page.locator("button[role='checkbox']");
        this.confirmBookingButton = page.locator("button:has-text('Confirm Booking')");
        this.bookingIdFilterInput = page.locator("input[placeholder='Booking ID']");
        this.applyFilterButton = page.locator("button:has-text('Apply Filter')");
    }

    /** Navigates here from the sidebar (present on every authenticated page). Lands on the "Group Fare" tab. */
    public void open() {
        page.getByText("Group Fare", new Page.GetByTextOptions().setExact(true)).first().click();
        page.waitForURL("**/group-fares", new Page.WaitForURLOptions().setTimeout(15000));
    }

    public boolean hasAvailableDeals() {
        try {
            bookNowButtons.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }

    /**
     * Books the first listed deal whose card shows at least {@code adults +
     * children} "seats available" (falling back to the first listed deal if
     * none qualifies) and raises the "Select Passengers" modal's Adult/Child
     * counts toward the requested totals (it defaults to 1 adult / 0
     * children). Picking a deal by displayed capacity - rather than always
     * the first one - avoids seat contention with other tests booking
     * against the same deal in parallel (this suite runs Group Fare's tests
     * concurrently). Each stepper's "Add" button still disables itself if
     * the deal's remaining capacity is used up regardless, so the requested
     * counts remain a ceiling, not a guarantee - the same as a human agent
     * clicking "+" until it stops responding. Use {@link #adultCount()}/
     * {@link #childCount()} afterward to read what was actually achieved
     * (read from the modal before it closes, since the counts aren't shown
     * again on the passenger-details form).
     */
    public void bookFirstAvailableDealWithPassengers(int adults, int children) {
        bookDealWithCapacity(adults + children).click();
        incrementUpTo(addAdultButton, adults - 1);
        incrementUpTo(addChildButton, children);

        achievedAdultCount = Integer.parseInt(adultCountLabel.textContent().trim());
        achievedChildCount = Integer.parseInt(childCountLabel.textContent().trim());

        addPassengerInfoButton.click();
        page.waitForURL("**/group-fares/form", new Page.WaitForURLOptions().setTimeout(15000));
    }

    private Locator bookDealWithCapacity(int minSeats) {
        Locator seatCounts = page.locator("span:has(img[src='/icons/flight-card/airplane-seat.svg'])");
        int dealCount = seatCounts.count();
        for (int i = 0; i < dealCount; i++) {
            int seats = Integer.parseInt(seatCounts.nth(i).textContent().trim());
            if (seats >= minSeats) {
                return bookNowButtons.nth(i);
            }
        }
        return bookNowButtons.first();
    }

    private void incrementUpTo(Locator addButton, int clicks) {
        for (int i = 0; i < clicks; i++) {
            if (!addButton.isEnabled()) {
                break;
            }
            addButton.click();
        }
    }

    public int adultCount() {
        return achievedAdultCount;
    }

    public int childCount() {
        return achievedChildCount;
    }

    public boolean isPassengerDetailsPageDisplayed() {
        try {
            page.getByText("Provide Passenger Details").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }

    /**
     * Downloads the "Template" spreadsheet, fills one row per passenger -
     * adults first, then children, each with unique name/passport details -
     * and uploads the result, which auto-fills every passenger-details field
     * below (title/name/DOB/passport/nationality/contact/email for every
     * passenger) in one shot. Never touches Confirm Booking.
     */
    public void fillPassengerDetailsFromSpreadsheet(int adultCount, int childCount) {
        Path template = downloadTemplate();
        Path filled = fillPassengerTemplate(template, adultCount, childCount);
        excelUploadInput.setInputFiles(filled);
    }

    private Path downloadTemplate() {
        Download download = page.waitForDownload(downloadTemplateLink::click);
        try {
            Path target = Files.createTempFile("group-fare-template-", ".xlsx");
            download.saveAs(target);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to download the Group Fare Pax Info template", e);
        }
    }

    /**
     * The template's row 2 is a worked example (Adult/Mr./Male/Bangladesh/
     * Passport/Bangladesh) whose hidden columns (C, E, G, K, N, R, T) are
     * VLOOKUPs translating the visible dropdown labels into the codes the
     * app expects (e.g. "Bangladesh" -> "BD"). Every filled row gets the
     * same formulas (rewritten for its own row number) rather than copying
     * row 2's cached ones, then the whole workbook is recalculated so the
     * saved file's cached values are correct even if the app's parser
     * doesn't recompute formulas itself.
     */
    private Path fillPassengerTemplate(Path templatePath, int adultCount, int childCount) {
        try (var in = Files.newInputStream(templatePath);
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheet(PASSENGERS_SHEET);

            long unique = System.currentTimeMillis() % 1_000_000;
            int excelRow = 2;
            for (int i = 1; i <= adultCount; i++) {
                fillPassengerRow(sheet, excelRow++, "Adult(s)", "Mr.", "Male",
                        "GroupAdult" + i, "AutoTest", LocalDate.now().minusYears(SAMPLE_ADULT_AGE_YEARS),
                        "AA" + String.format("%07d", unique + i), LocalDate.now().plusYears(5));
            }
            for (int i = 1; i <= childCount; i++) {
                fillPassengerRow(sheet, excelRow++, "Child(ren)", "Mstr.", "Male",
                        "GroupChild" + i, "AutoTest", LocalDate.now().minusYears(SAMPLE_CHILD_AGE_YEARS),
                        "AC" + String.format("%07d", unique + i), LocalDate.now().plusYears(5));
            }

            XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);

            Path filled = Files.createTempFile("group-fare-pax-filled-", ".xlsx");
            try (var out = Files.newOutputStream(filled)) {
                workbook.write(out);
            }
            return filled;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to fill the Group Fare Pax Info template", e);
        }
    }

    private void fillPassengerRow(Sheet sheet, int excelRow, String passengerType, String civility, String gender,
                                   String firstName, String lastName, LocalDate dateOfBirth,
                                   String passportNumber, LocalDate passportExpiryDate) {
        Row row = sheet.getRow(excelRow - 1);
        if (row == null) {
            row = sheet.createRow(excelRow - 1);
        }

        setCell(row, 1, passengerType);
        setCell(row, 3, civility);
        setCell(row, 5, gender);
        setCell(row, 7, firstName);
        setCell(row, 8, lastName);
        setCell(row, 9, "Bangladesh");
        setCell(row, 11, dateOfBirth.format(TEMPLATE_DATE_FORMAT));
        setCell(row, 12, "Passport");
        setCell(row, 14, passportNumber);
        setCell(row, 15, passportExpiryDate.format(TEMPLATE_DATE_FORMAT));
        setCell(row, 16, "Bangladesh");
        setCell(row, 18, "Bangladesh");
        setCell(row, 20, "01712345678");
        setCell(row, 21, (firstName + "." + lastName + excelRow + "@example.com").toLowerCase());

        row.createCell(2).setCellFormula("IFERROR(VLOOKUP(B" + excelRow + ",PassengerTypes!$A$2:$B$4, 2, 0), \"\")");
        row.createCell(4).setCellFormula("IFERROR(VLOOKUP(D" + excelRow + ",Civilities!$A$2:$B$14, 2, 0), \"\")");
        row.createCell(6).setCellFormula("IFERROR(VLOOKUP(F" + excelRow + ",Genders!$A$2:$B$4, 2, 0), \"\")");
        row.createCell(10).setCellFormula("IFERROR(VLOOKUP(J" + excelRow + ",Nationalities!$A$2:$B$241, 2, 0), \"\")");
        row.createCell(13).setCellFormula("IFERROR(VLOOKUP(M" + excelRow + ",DocumentTypes!$A$2:$B$6, 2, 0), \"\")");
        row.createCell(17).setCellFormula("IFERROR(VLOOKUP(Q" + excelRow + ",Countries!$A$2:$B$241, 2, 0), \"\")");
        row.createCell(19).setCellFormula("IFERROR(VLOOKUP(S" + excelRow + ",Countries!$A$2:$B$241, 2, 0), \"\")");
    }

    private void setCell(Row row, int columnIndex, String value) {
        var cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setCellValue(value);
    }

    public String firstPassengerFirstName() {
        return firstNameInput.inputValue();
    }

    /**
     * One accordion item per passenger, each collapsed after upload except
     * the first - passenger 2+'s input fields aren't in the DOM until their
     * item is expanded, so per-field inputs can't verify them. Each item's
     * header instead shows the passenger's full name once filled, which is
     * enough to confirm every row from the uploaded spreadsheet landed.
     */
    public List<String> passengerSummaries() {
        return page.locator("[data-slot='accordion-trigger'] span.truncate").allTextContents();
    }

    /** Count of passenger accordion items showing the green "complete" checkmark. */
    public int completedPassengerCount() {
        return page.locator("[data-slot='accordion-trigger'] svg.lucide-circle-check").count();
    }

    /**
     * "Adult"/"Child" badge per accordion item, in passenger order - this is
     * the app's own classification of each uploaded row, distinct from the
     * "Lead" badge (only on passenger 1) which uses a different background
     * color and is excluded by the {@code bg-white} class here.
     */
    public List<String> passengerTypeBadges() {
        return page.locator("[data-slot='accordion-trigger'] span.bg-white").allTextContents();
    }

    /** Toast/alert text shown after an upload (e.g. the "Passenger details imported from Excel" success toast). */
    public List<String> statusMessages() {
        return page.locator("[role='alert'], [data-sonner-toast]").allTextContents();
    }

    public void agreeToTerms() {
        agreeTermsCheckbox.click();
    }

    public boolean isConfirmBookingButtonVisible() {
        return confirmBookingButton.isVisible();
    }

    public boolean isConfirmBookingButtonEnabled() {
        return confirmBookingButton.isEnabled();
    }

    /**
     * CAUTION: submits a real reservation against live fare inventory and
     * charges the agent's account balance. Redirects to the "Booking List"
     * tab ({@code /group-fares/list}) on success - use {@link
     * #isBookingListPageDisplayed()} and the {@code latestBooking*} methods
     * to verify the new booking landed there.
     */
    public void confirmBooking() {
        confirmBookingButton.click();
    }

    public boolean isBookingListPageDisplayed() {
        try {
            page.waitForURL("**/group-fares/list", new Page.WaitForURLOptions().setTimeout(15000));
            return true;
        } catch (PlaywrightException timedOut) {
            return false;
        }
    }

    /**
     * Filters the Booking List down to the row(s) matching this exact
     * Booking ID via the "Booking ID" field + "Apply Filter" (typing alone
     * doesn't filter - the list only re-fetches once Apply Filter is
     * clicked). After this, {@link #latestBookingId()}/{@link
     * #latestBookingPassengerCount()}/{@link #latestBookingStatus()} read
     * the matching row instead of the most recent one.
     */
    public void searchBookingListByBookingId(String bookingId) {
        bookingIdFilterInput.fill(bookingId);
        applyFilterButton.click();
        page.waitForTimeout(1000);
    }

    public int bookingListRowCount() {
        return page.locator("tbody tr").count();
    }

    public String latestBookingId() {
        return latestBookingRowCell(BOOKING_LIST_COLUMN_ID);
    }

    public String latestBookingPassengerCount() {
        return latestBookingRowCell(BOOKING_LIST_COLUMN_PAX);
    }

    public String latestBookingStatus() {
        return latestBookingRowCell(BOOKING_LIST_COLUMN_STATUS);
    }

    /**
     * The first row is visible immediately as an empty/loading skeleton
     * while the list's data finishes fetching, so reading a cell right after
     * navigating here can race an empty string - poll the Booking ID cell
     * (first to render once data arrives) until it's non-empty before
     * reading any column off that row.
     */
    private String latestBookingRowCell(int columnIndex) {
        Locator firstRow = page.locator("tbody tr").first();
        firstRow.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        Locator bookingIdCell = firstRow.locator("td").nth(BOOKING_LIST_COLUMN_ID);

        long deadline = System.currentTimeMillis() + 15000;
        while (bookingIdCell.innerText().trim().isEmpty() && System.currentTimeMillis() < deadline) {
            page.waitForTimeout(300);
        }

        return firstRow.locator("td").nth(columnIndex).innerText().trim();
    }
}
