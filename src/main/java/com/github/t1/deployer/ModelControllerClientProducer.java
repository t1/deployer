package com.github.t1.deployer;

import java.io.IOException;
import java.net.InetAddress;

import javax.enterprise.inject.*;

import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.ModelControllerClient;

import com.github.t1.log.Logged;

@Slf4j
@Logged
public class ModelControllerClientProducer {
    @Produces
    ModelControllerClient produceModelControllerClient() throws IOException {
        InetAddress host = InetAddress.getByName("localhost");
        int port = 9999;
        log.info("connect to JBoss AS on {}:{}", host, port);
        return ModelControllerClient.Factory.create(host, port);
    }

    void closeModelControllerClient(@Disposes ModelControllerClient client) throws IOException {
        client.close();
    }
}
