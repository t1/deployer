package com.github.t1.deployer.app;

import io.swagger.jaxrs.config.BeanConfig;

import java.io.*;
import java.util.jar.*;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebServlet(loadOnStartup = 1)
public class SwaggerConfig extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        log.debug("servletConfig: {}", servletConfig);
        super.init(servletConfig);
        configureSwagger(servletConfig.getServletContext().getContextPath());
    }

    private void configureSwagger(String basePath) {
        BeanConfig config = new BeanConfig();
        config.setTitle("Deployer");
        config.setDescription("Deploys web archives to a JBoss web container");
        config.setVersion(apiVersion());
        config.setContact("t1 on github");
        config.setBasePath(basePath);
        config.setResourcePackage(getClass().getPackage().getName());
        log.debug("Swagger config: {}", config);
        registerAsScanner(config);
    }

    private String apiVersion() {
        ServletContext application = getServletConfig().getServletContext();
        try (InputStream inputStream = application.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            return retrieveVersion(new Manifest(inputStream));
        } catch (IOException e) {
            log.error("Could not determine API version", e);
            return "unknown";
        }
    }

    private String retrieveVersion(Manifest manifest) {
        Attributes versionAttributes = manifest.getMainAttributes();
        return versionAttributes.getValue("Implementation-Version");
    }

    private void registerAsScanner(BeanConfig swaggerConfig) {
        swaggerConfig.setScan(true);
    }
}
