package com.takeoff.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.takeoff.config.ConfigManager;

/**
 * Owns one Playwright/Browser/BrowserContext/Page per thread so TestNG can
 * run tests in parallel (see testng.xml: parallel="methods") without tests
 * sharing browser state.
 */
public final class PlaywrightFactory {

    private static final ThreadLocal<Playwright> PLAYWRIGHT = new ThreadLocal<>();
    private static final ThreadLocal<Browser> BROWSER = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();

    private PlaywrightFactory() {
    }

    public static Page initBrowser() {
        Playwright playwright = Playwright.create();
        PLAYWRIGHT.set(playwright);

        boolean headless = ConfigManager.headless();

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(ConfigManager.slowMoMs());
        // Headed runs: let Chrome open at its natural OS window size instead of a
        // fixed viewport, otherwise the page can render cropped/mismatched against
        // the actual window on screens that aren't exactly viewport-sized.
        if (!headless) {
            launchOptions.setArgs(java.util.List.of("--start-maximized"));
        }

        Browser browser = switch (ConfigManager.browser().toLowerCase()) {
            case "firefox" -> playwright.firefox().launch(launchOptions);
            case "webkit" -> playwright.webkit().launch(launchOptions);
            default -> playwright.chromium().launch(launchOptions);
        };
        BROWSER.set(browser);

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();
        if (headless) {
            contextOptions.setViewportSize(ConfigManager.viewportWidth(), ConfigManager.viewportHeight());
        } else {
            contextOptions.setViewportSize(null);
        }
        BrowserContext context = browser.newContext(contextOptions);
        context.setDefaultTimeout(ConfigManager.defaultTimeoutMs());
        CONTEXT.set(context);

        Page page = context.newPage();
        PAGE.set(page);

        return page;
    }

    public static Page getPage() {
        Page page = PAGE.get();
        if (page == null) {
            throw new IllegalStateException("Playwright Page not initialized for this thread. Call initBrowser() first.");
        }
        return page;
    }

    public static void closeBrowser() {
        try {
            if (CONTEXT.get() != null) {
                CONTEXT.get().close();
            }
            if (BROWSER.get() != null) {
                BROWSER.get().close();
            }
            if (PLAYWRIGHT.get() != null) {
                PLAYWRIGHT.get().close();
            }
        } finally {
            CONTEXT.remove();
            BROWSER.remove();
            PLAYWRIGHT.remove();
            PAGE.remove();
        }
    }
}
