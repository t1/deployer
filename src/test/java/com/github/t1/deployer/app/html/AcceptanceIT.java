package com.github.t1.deployer.app.html;

import static com.gargoylesoftware.htmlunit.BrowserVersion.*;
import static org.junit.Assert.*;

import org.junit.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

@Ignore
public class AcceptanceIT {
    private final WebDriver driver = driver();

    public WebDriver driver() {
        HtmlUnitDriver driver = new HtmlUnitDriver(FIREFOX_24);
        driver.setJavascriptEnabled(true);
        return driver;
    }

    @After
    public void cleanup() {
        driver.quit();
    }

    @Test
    public void shouldRedirectIndexToDeployments() {
        driver.get("http://localhost:8080/deployer");

        assertEquals("http://localhost:8080/deployer/deployments/*", driver.getCurrentUrl());
        assertEquals("Deployments", driver.getTitle());
    }
}
