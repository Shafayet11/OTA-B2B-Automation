package com.takeoff.listeners;

import com.microsoft.playwright.Page;
import com.takeoff.core.PlaywrightFactory;
import io.qameta.allure.Allure;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;

public class TestListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        try {
            Page page = PlaywrightFactory.getPage();
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            Allure.addAttachment(
                    result.getMethod().getMethodName() + " - failure screenshot",
                    new ByteArrayInputStream(screenshot));
        } catch (IllegalStateException e) {
            // No page for this thread (e.g. failure happened before setUp) - nothing to attach.
        }
    }
}
