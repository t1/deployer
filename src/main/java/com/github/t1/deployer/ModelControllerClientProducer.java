package com.github.t1.deployer;

import java.io.IOException;

import javax.enterprise.inject.*;

import org.jboss.as.controller.client.ModelControllerClient;

import com.github.t1.log.Logged;

@Logged
public class ModelControllerClientProducer {
    @Produces
    ModelControllerClient produceModelControllerClient() throws IOException {
        return ModelControllerClient.Factory.create("127.0.0.1", 9990);
    }

    void closeModelControllerClient(@Disposes ModelControllerClient client) throws IOException {
        client.close();
    }
}
