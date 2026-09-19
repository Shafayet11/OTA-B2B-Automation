package com.takeoff.tests;

import com.takeoff.base.AuthenticatedTest;
import com.takeoff.pages.SearchPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;

@Epic("B2B OTA Portal")
@Feature("Search")
public class SearchTests extends AuthenticatedTest {

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Description("Searching one-way flights between two valid cities on a future date returns at least one result.")
    public void oneWayFlightSearchReturnsResults() {
        SearchPage searchPage = new SearchPage(page);

        searchPage.searchOneWayFlight("Dhaka", "DAC", "Dubai", "DXB", LocalDate.now().plusMonths(1));

        Assert.assertTrue(searchPage.resultsCount() > 0, "Expected at least one search result");
    }
}
