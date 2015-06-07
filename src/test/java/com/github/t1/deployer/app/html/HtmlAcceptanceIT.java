package com.github.t1.deployer.app.html;

import static com.gargoylesoftware.htmlunit.BrowserVersion.*;
import static org.junit.Assert.*;

import java.io.File;

import javax.ws.rs.core.UriInfo;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

@Ignore
@RunAsClient
@RunWith(Arquillian.class)
public class HtmlAcceptanceIT {
    @Deployment
    public static WebArchive createDeployment() {
        File file = new File("target/deployer.war");
        WebArchive war = ShrinkWrap.createFromZipFile(WebArchive.class, file);
        System.out.println("---- contents of " + file + ":\n" + war.toString(true));
        return war;
    }

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
    public void shouldRedirectIndexToDeployments(@ArquillianResource UriInfo uri) {
        driver.get(uri.toString());

        assertEquals(uri + "/deployments/*", driver.getCurrentUrl());
        assertEquals("Deployments", driver.getTitle());
    }
}
