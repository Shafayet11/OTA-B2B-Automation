# Takeoff B2B OTA Test Automation

UI test automation for the Takeoff B2B agent portal, built with Java 21 + Playwright + TestNG + Allure, following the Page Object Model.

## Stack

- **Playwright (Java)** — browser automation
- **TestNG** — test runner, parallel execution
- **Allure** — HTML reporting with screenshots on failure
- **Maven** — build/dependency management

## Project layout

```
src/main/java/com/takeoff/
  config/       ConfigManager        - loads config.properties, env-var overrides
  core/         PlaywrightFactory    - per-thread browser/context/page lifecycle
  base/         BaseTest             - @BeforeMethod/@AfterMethod setup+teardown (B2B agent portal)
                AuthenticatedTest    - BaseTest + logs in, for tests that need a session
                AdminBaseTest        - same as BaseTest, but for the Admin portal
                AdminAuthenticatedTest - AdminBaseTest + logs in as admin
  pages/        Page Objects         - HomePage, LoginPage, SearchPage, BookingPage,
                                       PaymentRequestPage (B2B agent portal, shadcn/radix
                                       forms - see "Notable gotchas" below), AdminLoginPage,
                                       TopupApprovalPage (Admin portal, Angular forms), and
                                       BasePage (shared helpers)
  listeners/    TestListener         - attaches a screenshot to Allure on failure

src/test/java/com/takeoff/tests/     - LoginTests, SearchTests, BookingTests, TopupRequestTests, TopupApprovalTests
src/test/resources/config.properties - default config (no real secrets committed)
src/test/resources/attachments/      - dummy file used for required file-upload fields
testng.xml                           - full suite, parallel="methods"
testng-smoke.xml                     - quick single-test runner, handy while iterating
```

## Covered flows

- **Login** — valid credentials (`LoginTests.validCredentialsLogInSuccessfully`)
- **Search** — one-way flight search returns results (`SearchTests.oneWayFlightSearchReturnsResults`)
- **Booking** — search → select a fare → reach the traveler-details form (`BookingTests.agentCanReachBookingForm`)
- **Booking, full traveler details** — search → select a fare → fill every
  required traveler-details field → the booking is ready to confirm, for
  both fare-selection paths a result card offers:
  - **Regular fare** — the direct "Select" button on a specific booking
    class (`SearchPage.selectResult`, `BookingTests.agentCanCompleteBookingForm_RegularFare`)
  - **Branded fare** — the "View Price" fare-family modal (Economy Lite/
    Saver/Value/Flex, etc.) (`SearchPage.selectFirstBrandedFare`,
    `BookingTests.agentCanCompleteBookingForm_BrandedFare`)

⚠️ **BookingTests never clicks "Confirm Booking."** That step submits a real
reservation against live fare inventory and charges the agent's account
balance on this environment — there is no sandbox/mock GDS here. Every
booking test stops at the traveler-details screen (the full-details tests
fill every required field and assert Confirm Booking is enabled, but never
click it). `BookingPage.confirmBooking()` exists only for manual/exploratory
use — do not call it from automated tests unless the environment changes
and this is explicitly re-confirmed.

- **Payment Request** (`/topup-request`) — an agent submits a deposit/top-up
  request for someone else to review. Unlike "Confirm Booking," submitting
  here does not move money by itself, so `TopupRequestTests` clicks Submit
  for real; each run leaves a new request record behind. All 6 deposit types
  (Cheque, Bank Deposit, Bank Transfer, Cash, Bkash, Nagad) have fill methods
  on `PaymentRequestPage`; **Cash, Bkash and Nagad** currently work
  end-to-end in this environment — see "Known environment bugs" below for
  why Cheque, Bank Deposit and Bank Transfer are still disabled.

- **Topup Approval** (Admin portal &gt; Balance Management &gt; Topup &gt;
  View Request) — an admin reviews a top-up request an agent submitted.
  `TopupApprovalTests` logs in as the B2B agent, submits a fresh request,
  then switches to the Admin portal to open it, fill Admin Reference/Remarks,
  and assert Approve is enabled. Covers the 3 deposit types that currently
  submit successfully (Cash, Bkash, Nagad) — same reasoning as Payment
  Request above for why Cheque/Bank Deposit/Bank Transfer aren't covered
  (no request to review if submission itself is blocked).

⚠️ **TopupApprovalTests never clicks "Approve."** Approving credits the
agent's account balance for real — same category as Booking's "Confirm
Booking." Tests fill every required field and assert Approve is enabled,
but never click it. `TopupApprovalPage.approve()` exists only for manual/
exploratory use — do not call it from automated tests unless this is
explicitly re-confirmed.

The Admin portal also gates login behind a TOTP "Authenticator Setup" QR
screen and, when actually approving, a separate Admin PIN dialog (see
`admin.pin` below) — neither blocks the read/fill flow these tests use;
see `AdminLoginPage` and `TopupApprovalPage` javadoc for details.

## Known environment bugs (not test issues)

Found while building `TopupRequestTests` - disabled with a comment pointing
back here; flip `enabled = true` once fixed:

- **"Deposited In" dropdown is empty.** Cheque, Bank Deposit and Bank
  Transfer all require picking a bank account here, but the dropdown has zero
  options — confirmed via the underlying native `<select>` also having zero
  `<option>`s (not just a slow-to-populate list) across repeated checks, even
  though "Partner Bank Details" clearly lists 6 valid accounts to deposit
  into. Blocks 3 of the 6 deposit types. Still reproducing as of 2026-09-16.

~~Bkash/Nagad submission failing with "Gateway service charge amount has
been modified"~~ — fixed as of 2026-09-16; both now pass end-to-end.

## One-time setup

1. Install Playwright's browser binaries:
   ```
   mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
   ```
2. Configure the site URLs and test credentials. Every key in `config.properties`
   can be overridden by an environment variable, so real values never need to
   be committed:

   | Property        | Env var override |
   |-----------------|-------------------|
   | `b2b.url`       | `B2B_URL`         |
   | `admin.url`     | `ADMIN_URL`       |
   | `email`         | `TEST_EMAIL`      |
   | `password`      | `TEST_PASSWORD`   |
   | `admin.email`   | `ADMIN_EMAIL`     |
   | `admin.password`| `ADMIN_PASSWORD`  |
   | `admin.pin`     | `ADMIN_PIN`       |

   `admin.*` are a separate Admin-portal account/role from `email`/`password`
   above, used by `TopupApprovalTests`. `admin.pin` is the PIN the Admin
   portal asks for as a second confirmation step before actually approving a
   request — not needed for the fill/assert-enabled flow the tests use, but
   configured for completeness/manual use.

   PowerShell example (persists across terminal/IDE restarts):
   ```powershell
   setx B2B_URL "http://192.168.61.115:8001/"
   setx TEST_EMAIL "agent@example.com"
   setx TEST_PASSWORD "********"
   ```
   Restart VS Code/your terminal after `setx` for the new values to be picked up.

## Running tests

```
mvn test
```

Runs headed by default (`headless=false` in `config.properties`) so you can watch
it work; the browser opens maximized rather than at a fixed size. For CI/fast
runs:
```
mvn test -Dheadless=true
```

Run a single test quickly while iterating:
```
mvn test -DsuiteXmlFile=testng-smoke.xml
```
(edit `testng-smoke.xml`'s `<include name="...">` to point at the method you want)

## Viewing the Allure report

```
mvn allure:serve
```

## CI

`.github/workflows/ci.yml` runs the suite on every push/PR, installing Playwright
browsers and uploading Allure results + Surefire reports as build artifacts.
Set `B2B_URL`, `ADMIN_URL`, `TEST_EMAIL`, `TEST_PASSWORD` as repository secrets for CI runs.

## Notable gotchas already worked around (see Page Objects for details)

- A promotional dialog can pop up on both the homepage and the post-login
  page, blocking clicks until dismissed (`HomePage.dismissPromoDialogIfPresent()`).
- The origin/destination location pickers leave the previous popover's input
  mounted (hidden) in the DOM — locators must scope to `.last()`.
- The date picker shows two months side by side, but the second (upcoming)
  month isn't part of the accessibility tree until "Next Month" is clicked once.
- When building a Playwright `Pattern` for `getByRole`/`getByText`, never use
  `Pattern.quote()` — its `\Q..\E` syntax is Java-only and is sent as-is to the
  browser's JavaScript regex engine, silently matching nothing.
- Across the traveler-details and payment-request forms, most fields have no
  `name`/`id` and share generic placeholders, but each renders as a control
  plus a label, both direct children of a `div.relative` wrapper - the label
  sits after the control on some forms (a floating `<span>`) and before it on
  others (a plain `<label>`). `BasePage.fieldByLabel(String)` scopes to that
  wrapper to identify an individual field regardless of which pattern the
  form uses; `BasePage.selectDropdown` and `BasePage.pickDate` build on it for
  the shadcn/radix Select and calendar-popover components shared across pages.
- Selecting a Title (Mr/Ms/Mrs) auto-derives and disables Gender — don't try
  to set Gender independently.
- The date pickers (DOB, passport issue/expiry, deposit date, ...) have
  Month/Year dropdowns for fast navigation, but the Month dropdown's
  enabled/disabled options are computed against whichever year is currently
  showing — `BasePage.pickDate` picks the Year first to avoid landing on a
  month that's disabled for the stale default year. It also tries clicking
  the target day directly before navigating at all, since the day is often
  already in the calendar's default view (e.g. today, for a deposit-date
  field) - worth keeping fast on the Payment Request forms in particular,
  where a slow multi-step date entry can trip the "gateway service charge
  amount has been modified" error (see "Known environment bugs").
- Deposit types on Payment Request that use `name="reference"` show a
  different label per type ("Reference" vs "Reference Number") - locate by
  the `name` attribute rather than label text where one exists.
- The Payment Request attachment dropzone has no visible required-field
  asterisk, but submission is rejected with "Attachment is required!"
  without one — always attach a file (see `src/test/resources/attachments/`).
