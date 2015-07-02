package com.github.t1.deployer.app;

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

import com.github.t1.rest.RestResource;

@Ignore
@RunAsClient
@RunWith(Arquillian.class)
public class RestAcceptanceIT {
    @Deployment
    public static WebArchive createDeployment() {
        File file = new File("target/deployer.war");
        WebArchive war = ShrinkWrap.createFromZipFile(WebArchive.class, file);
        System.out.println("---- contents of " + file + ":\n" + war.toString(true));
        return war;
    }

    @ArquillianResource
    UriInfo uri;

    @Test
    public void shouldGetIndex() {
        System.out.println("-------------------------- get index from " + uri);
        String entity = new RestResource(uri.getBaseUri()).get(String.class);

        assertEquals("", entity);
    }
}
