package com.github.t1.deployer.app;

import static org.junit.Assert.*;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import com.github.t1.rest.RestResource;

@Ignore
@RunWith(Arquillian.class)
public class ArquillianIT {
    @Deployment
    public static WebArchive createDeployment() {
        File file = new File("target/deployer.war");
        WebArchive war = ShrinkWrap.createFromZipFile(WebArchive.class, file);
        System.out.println("---- contents of " + file + ":\n" + war.toString(true));
        return war;
    }

    @Test
    public void shouldGetIndex() {
        System.out.println("-------------------------- get index");
        String entity = new RestResource("http://localhost:8080/deployer").get(String.class);

        assertEquals("", entity);
    }
}
