package com.github.t1.deployer;

import java.io.IOException;
import java.net.*;

import javax.enterprise.inject.*;

import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.ModelControllerClient;

@Slf4j
public class ModelControllerClientProducer {
    @Produces
    ModelControllerClient produce() throws UnknownHostException {
        log.debug("produce model controller client");
        ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9990);
        log.debug("produced model controller client: {}", client);
        return client;
    }

    void close(@Disposes ModelControllerClient client) throws IOException {
        log.debug("close model controller client: {}", client);
        client.close();
        log.debug("closed model controller client: {}", client);
    }
}
