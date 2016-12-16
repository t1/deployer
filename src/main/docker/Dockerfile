FROM jboss/wildfly
ADD deployer.war ${JBOSS_HOME}/standalone/deployments/
ADD deployer.config.yaml ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml
ADD deployer.root.bundle ${JBOSS_HOME}/standalone/configuration/deployer.root.bundle
RUN ${JBOSS_HOME}/bin/standalone.sh --server-config "standalone-full.xml" # shutdown-after-boot must be set
RUN rm -r \
    ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml \
    ${JBOSS_HOME}/standalone/configuration/deployer.root.bundle \
    ${JBOSS_HOME}/standalone/configuration/standalone_xml_history
ADD deployer.config.readonly.yaml ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "--server-config", "standalone-full.xml", "-b", "0.0.0.0"]
