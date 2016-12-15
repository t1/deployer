FROM jboss/wildfly
ADD ../../target/deployer.war ${JBOSS_HOME}/standalone/deployments/
ADD deployer.config.yaml ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml
ADD deployer.root.bundle ${JBOSS_HOME}/standalone/configuration/deployer.root.bundle
RUN ${JBOSS_HOME}/bin/standalone.sh --server-config "standalone-full.xml" # shutdown-after-boot must be set
RUN rm -r \
    ${JBOSS_HOME}/standalone/deployments/deployer.war \
    ${JBOSS_HOME}/standalone/deployments/deployer.war.deployed \
    ${JBOSS_HOME}/standalone/configuration/deployer.config.yaml \
    ${JBOSS_HOME}/standalone/configuration/deployer.root.bundle \
    ${JBOSS_HOME}/standalone/configuration/standalone_xml_history
EXPOSE 8080 9990
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "--server-config", "standalone-full.xml", "-b", "0.0.0.0"]
