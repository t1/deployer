FROM jboss/wildfly
LABEL maintainer=https://github.com/t1 license=Apache-2.0 name= build-date= vendor=

COPY target/deployer.war ${JBOSS_HOME}/standalone/deployments/
COPY src/main/docker/deployer.config.build.yaml ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml
COPY src/main/docker/deployer.root.build.bundle ${JBOSS_HOME}/standalone/configuration/deployer.root.bundle
ENV SERVER_CONFIG=standalone-full.xml
RUN ${JBOSS_HOME}/bin/standalone.sh --server-config ${SERVER_CONFIG} # shutdown-after-boot must be set
RUN rm -r \
    ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml \
    ${JBOSS_HOME}/standalone/configuration/deployer.root.bundle \
    ${JBOSS_HOME}/standalone/configuration/standalone_xml_history
COPY src/main/docker/deployer.config.run.yaml ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml
COPY src/main/docker/deployer.root.run.bundle ${JBOSS_HOME}/standalone/configuration/deployer.root.bundle

CMD ${JBOSS_HOME}/bin/standalone.sh --server-config ${SERVER_CONFIG} -b 0.0.0.0
