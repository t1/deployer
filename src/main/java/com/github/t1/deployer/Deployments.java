package com.github.t1.deployer;

import java.io.*;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

@Slf4j
@Path("/deployments")
public class Deployments {

    @Inject
    ModelControllerClient client;

    @GET
    public Response getDeployments() throws IOException {
        List<String> list = new ArrayList<>();

        ModelNode op = new ModelNode();
        op.get("operation").set("read-resource-description");

        ModelNode address = op.get("address");
        address.add("subsystem", "web");
        address.add("connector", "http");

        op.get("recursive").set(true);
        op.get("operations").set(true);

        ModelNode returnVal = client.execute(op);
        log.info("{}", returnVal.get("result").toString());

        return Response.ok(list).build();
    }

    @GET
    @Path("/versions")
    public Response getVersions() {
        List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        list.add("three");
        return Response.ok(list).build();
    }

    @GET
    @Path("/pom")
    public Response getPom() {
        String out;
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            try (FileReader fileReader = new FileReader("pom.xml")) {
                Model model = reader.read(fileReader);
                out = model.toString();
            }
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
        return Response.ok(out).build();
    }
}
