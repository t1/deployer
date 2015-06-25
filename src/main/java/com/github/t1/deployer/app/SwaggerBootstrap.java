package com.github.t1.deployer.app;

import io.swagger.config.*;
import io.swagger.core.filter.SwaggerSpecFilter;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.*;

import java.util.Set;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import lombok.extern.slf4j.Slf4j;

import com.github.t1.log.VersionLogContextVariableProducer;

@Slf4j
@WebServlet(loadOnStartup = 1)
public class SwaggerBootstrap extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    VersionLogContextVariableProducer versionProducer;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        configureSwagger(servletConfig.getServletContext().getContextPath());
    }

    private void configureSwagger(String basePath) {
        BeanConfig config = new BeanConfig() {
            @Override
            public Swagger configure(Swagger swagger) {
                configureFilter();
                configureInfo(swagger);
                configureSchemes(swagger);
                return swagger.host(getHost()).basePath(getBasePath()); // don't overwrite info
            }

            private void configureFilter() {
                if (getFilterClass() != null) {
                    try {
                        SwaggerSpecFilter filter = (SwaggerSpecFilter) Class.forName(getFilterClass()).newInstance();
                        if (filter != null) {
                            FilterFactory.setFilter(filter);
                        }
                    } catch (Exception e) {
                        log.error("failed to load filter", e);
                    }
                }
            }

            private void configureInfo(Swagger swagger) {
                if (getVersion() != null) {
                    swagger.getInfo().setVersion(getVersion());
                }
            }

            private void configureSchemes(Swagger swagger) {
                if (getSchemes() != null) {
                    for (String scheme : getSchemes()) {
                        swagger.scheme(Scheme.forValue(scheme));
                    }
                }
            }
        };
        String version = versionProducer.version().value();
        log.debug("found version {}", version);
        config.setVersion(version);
        config.setBasePath(basePath);
        config.setResourcePackage(getClass().getPackage().getName());
        registerAsScanner(config);
    }

    private void registerAsScanner(BeanConfig swaggerConfig) {
        Set<Class<?>> classes = swaggerConfig.classes();
        if (classes != null) {
            new Reader(swaggerConfig.getSwagger()).read(classes);
        }
        ScannerFactory.setScanner(swaggerConfig);
    }
}
