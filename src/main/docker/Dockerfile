FROM jboss/wildfly
MAINTAINER https://github.com/t1

COPY deployer.war ${JBOSS_HOME}/standalone/deployments/
COPY deployer.config.yaml ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml
COPY deployer.root.bundle ${JBOSS_HOME}/standalone/configuration/deployer.root.bundle
ENV SERVER_CONFIG=standalone-full.xml
RUN ${JBOSS_HOME}/bin/standalone.sh --server-config ${SERVER_CONFIG} # shutdown-after-boot must be set
RUN rm -r \
    ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml \
    ${JBOSS_HOME}/standalone/configuration/deployer.root.bundle \
    ${JBOSS_HOME}/standalone/configuration/standalone_xml_history
COPY deployer.config.readonly.yaml ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml

CMD ${JBOSS_HOME}/bin/standalone.sh --server-config ${SERVER_CONFIG} -b 0.0.0.0
