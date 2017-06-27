#!/usr/bin/env bash

m2=/Users/rdohna/.m2/repository

java -cp target/test-classes:target/classes:\
${m2}/com/github/t1/test-tools/1.5.0/test-tools-1.5.0.jar:\
${m2}/com/github/t1/graph/1.0.1/graph-1.0.1.jar:\
${m2}/org/jboss/shrinkwrap/shrinkwrap-api/1.2.6/shrinkwrap-api-1.2.6.jar:\
${m2}/org/jboss/shrinkwrap/resolver/shrinkwrap-resolver-api-maven/2.2.6/shrinkwrap-resolver-api-maven-2.2.6.jar:\
${m2}/org/jboss/shrinkwrap/resolver/shrinkwrap-resolver-api/2.2.6/shrinkwrap-resolver-api-2.2.6.jar:\
${m2}/org/wildfly/core/wildfly-controller-client/2.2.0.Final/wildfly-controller-client-2.2.0.Final.jar:\
${m2}/org/wildfly/core/wildfly-protocol/2.2.0.Final/wildfly-protocol-2.2.0.Final.jar:\
${m2}/org/jboss/remoting/jboss-remoting/4.0.21.Final/jboss-remoting-4.0.21.Final.jar:\
${m2}/org/jboss/sasl/jboss-sasl/1.0.5.Final/jboss-sasl-1.0.5.Final.jar:\
${m2}/org/jboss/xnio/xnio-api/3.4.0.Final/xnio-api-3.4.0.Final.jar:\
${m2}/org/wildfly/common/wildfly-common/1.1.0.Final/wildfly-common-1.1.0.Final.jar:\
${m2}/org/jboss/xnio/xnio-nio/3.4.0.Final/xnio-nio-3.4.0.Final.jar:\
${m2}/org/jboss/jboss-dmr/1.3.0.Final/jboss-dmr-1.3.0.Final.jar:\
${m2}/org/jboss/logging/jboss-logging/3.3.0.Final/jboss-logging-3.3.0.Final.jar:\
${m2}/org/jboss/threads/jboss-threads/2.2.1.Final/jboss-threads-2.2.1.Final.jar:\
${m2}/org/wildfly/plugins/wildfly-plugin-core/1.1.0.Final/wildfly-plugin-core-1.1.0.Final.jar:\
${m2}/org/wildfly/core/wildfly-launcher/1.0.1.Final/wildfly-launcher-1.0.1.Final.jar:\
${m2}/commons-codec/commons-codec/1.9/commons-codec-1.9.jar:\
${m2}/org/apache/httpcomponents/httpclient-cache/4.5.2/httpclient-cache-4.5.2.jar:\
${m2}/com/github/t1/problem-detail/1.0.2/problem-detail-1.0.2.jar:\
${m2}/com/github/t1/logging-interceptor/3.1.5/logging-interceptor-3.1.5.jar:\
${m2}/com/github/t1/stereotype-helper/1.0.3/stereotype-helper-1.0.3.jar:\
${m2}/org/javassist/javassist/3.21.0-GA/javassist-3.21.0-GA.jar:\
${m2}/com/fasterxml/jackson/core/jackson-databind/2.8.7/jackson-databind-2.8.7.jar:\
${m2}/com/fasterxml/jackson/core/jackson-annotations/2.8.0/jackson-annotations-2.8.0.jar:\
${m2}/com/fasterxml/jackson/core/jackson-core/2.8.7/jackson-core-2.8.7.jar:\
${m2}/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.8.7/jackson-dataformat-yaml-2.8.7.jar:\
${m2}/org/yaml/snakeyaml/1.17/snakeyaml-1.17.jar:\
${m2}/com/google/guava/guava/21.0/guava-21.0.jar:\
${m2}/org/jdom/jdom2/2.0.6/jdom2-2.0.6.jar:\
${m2}/com/github/t1/xml/0.0.5/xml-0.0.5.jar:\
${m2}/ch/qos/logback/logback-classic/1.2.1/logback-classic-1.2.1.jar:\
${m2}/ch/qos/logback/logback-core/1.2.1/logback-core-1.2.1.jar:\
${m2}/io/dropwizard/dropwizard-testing/1.0.6/dropwizard-testing-1.0.6.jar:\
${m2}/io/dropwizard/dropwizard-core/1.0.6/dropwizard-core-1.0.6.jar:\
${m2}/io/dropwizard/dropwizard-util/1.0.6/dropwizard-util-1.0.6.jar:\
${m2}/com/google/code/findbugs/jsr305/3.0.1/jsr305-3.0.1.jar:\
${m2}/joda-time/joda-time/2.9.4/joda-time-2.9.4.jar:\
${m2}/io/dropwizard/dropwizard-jackson/1.0.6/dropwizard-jackson-1.0.6.jar:\
${m2}/com/fasterxml/jackson/datatype/jackson-datatype-guava/2.7.8/jackson-datatype-guava-2.7.8.jar:\
${m2}/com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.7.8/jackson-datatype-jsr310-2.7.8.jar:\
${m2}/com/fasterxml/jackson/datatype/jackson-datatype-jdk8/2.7.8/jackson-datatype-jdk8-2.7.8.jar:\
${m2}/com/fasterxml/jackson/module/jackson-module-afterburner/2.7.8/jackson-module-afterburner-2.7.8.jar:\
${m2}/com/fasterxml/jackson/datatype/jackson-datatype-joda/2.7.8/jackson-datatype-joda-2.7.8.jar:\
${m2}/io/dropwizard/dropwizard-validation/1.0.6/dropwizard-validation-1.0.6.jar:\
${m2}/org/hibernate/hibernate-validator/5.2.4.Final/hibernate-validator-5.2.4.Final.jar:\
${m2}/com/fasterxml/classmate/1.1.0/classmate-1.1.0.jar:\
${m2}/org/glassfish/javax.el/3.0.0/javax.el-3.0.0.jar:\
${m2}/io/dropwizard/dropwizard-configuration/1.0.6/dropwizard-configuration-1.0.6.jar:\
${m2}/org/apache/commons/commons-lang3/3.4/commons-lang3-3.4.jar:\
${m2}/io/dropwizard/dropwizard-logging/1.0.6/dropwizard-logging-1.0.6.jar:\
${m2}/io/dropwizard/metrics/metrics-logback/3.1.2/metrics-logback-3.1.2.jar:\
${m2}/org/slf4j/jul-to-slf4j/1.7.21/jul-to-slf4j-1.7.21.jar:\
${m2}/org/slf4j/log4j-over-slf4j/1.7.21/log4j-over-slf4j-1.7.21.jar:\
${m2}/org/slf4j/jcl-over-slf4j/1.7.21/jcl-over-slf4j-1.7.21.jar:\
${m2}/org/eclipse/jetty/jetty-util/9.3.9.v20160517/jetty-util-9.3.9.v20160517.jar:\
${m2}/io/dropwizard/dropwizard-metrics/1.0.6/dropwizard-metrics-1.0.6.jar:\
${m2}/io/dropwizard/dropwizard-jersey/1.0.6/dropwizard-jersey-1.0.6.jar:\
${m2}/org/glassfish/jersey/ext/jersey-metainf-services/2.23.2/jersey-metainf-services-2.23.2.jar:\
${m2}/org/glassfish/jersey/ext/jersey-bean-validation/2.23.2/jersey-bean-validation-2.23.2.jar:\
${m2}/io/dropwizard/metrics/metrics-jersey2/3.1.2/metrics-jersey2-3.1.2.jar:\
${m2}/com/fasterxml/jackson/jaxrs/jackson-jaxrs-json-provider/2.7.8/jackson-jaxrs-json-provider-2.7.8.jar:\
${m2}/com/fasterxml/jackson/jaxrs/jackson-jaxrs-base/2.7.8/jackson-jaxrs-base-2.7.8.jar:\
${m2}/com/fasterxml/jackson/module/jackson-module-jaxb-annotations/2.7.8/jackson-module-jaxb-annotations-2.7.8.jar:\
${m2}/org/glassfish/jersey/containers/jersey-container-servlet/2.23.2/jersey-container-servlet-2.23.2.jar:\
${m2}/org/eclipse/jetty/jetty-server/9.3.9.v20160517/jetty-server-9.3.9.v20160517.jar:\
${m2}/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar:\
${m2}/org/eclipse/jetty/jetty-io/9.3.9.v20160517/jetty-io-9.3.9.v20160517.jar:\
${m2}/org/eclipse/jetty/jetty-webapp/9.3.9.v20160517/jetty-webapp-9.3.9.v20160517.jar:\
${m2}/org/eclipse/jetty/jetty-xml/9.3.9.v20160517/jetty-xml-9.3.9.v20160517.jar:\
${m2}/org/eclipse/jetty/jetty-continuation/9.3.9.v20160517/jetty-continuation-9.3.9.v20160517.jar:\
${m2}/io/dropwizard/dropwizard-servlets/1.0.6/dropwizard-servlets-1.0.6.jar:\
${m2}/io/dropwizard/metrics/metrics-annotation/3.1.2/metrics-annotation-3.1.2.jar:\
${m2}/io/dropwizard/dropwizard-jetty/1.0.6/dropwizard-jetty-1.0.6.jar:\
${m2}/io/dropwizard/metrics/metrics-jetty9/3.1.2/metrics-jetty9-3.1.2.jar:\
${m2}/org/eclipse/jetty/jetty-servlet/9.3.9.v20160517/jetty-servlet-9.3.9.v20160517.jar:\
${m2}/org/eclipse/jetty/jetty-security/9.3.9.v20160517/jetty-security-9.3.9.v20160517.jar:\
${m2}/org/eclipse/jetty/jetty-servlets/9.3.9.v20160517/jetty-servlets-9.3.9.v20160517.jar:\
${m2}/org/eclipse/jetty/jetty-http/9.3.9.v20160517/jetty-http-9.3.9.v20160517.jar:\
${m2}/io/dropwizard/dropwizard-lifecycle/1.0.6/dropwizard-lifecycle-1.0.6.jar:\
${m2}/io/dropwizard/metrics/metrics-core/3.1.2/metrics-core-3.1.2.jar:\
${m2}/io/dropwizard/metrics/metrics-jvm/3.1.2/metrics-jvm-3.1.2.jar:\
${m2}/io/dropwizard/metrics/metrics-servlets/3.1.2/metrics-servlets-3.1.2.jar:\
${m2}/io/dropwizard/metrics/metrics-json/3.1.2/metrics-json-3.1.2.jar:\
${m2}/io/dropwizard/metrics/metrics-healthchecks/3.1.2/metrics-healthchecks-3.1.2.jar:\
${m2}/io/dropwizard/dropwizard-request-logging/1.0.6/dropwizard-request-logging-1.0.6.jar:\
${m2}/ch/qos/logback/logback-access/1.1.7/logback-access-1.1.7.jar:\
${m2}/net/sourceforge/argparse4j/argparse4j/0.7.0/argparse4j-0.7.0.jar:\
${m2}/org/eclipse/jetty/toolchain/setuid/jetty-setuid-java/1.0.3/jetty-setuid-java-1.0.3.jar:\
${m2}/org/objenesis/objenesis/2.3/objenesis-2.3.jar:\
${m2}/org/glassfish/jersey/test-framework/providers/jersey-test-framework-provider-inmemory/2.23.2/jersey-test-framework-provider-inmemory-2.23.2.jar:\
${m2}/org/glassfish/jersey/test-framework/jersey-test-framework-core/2.23.2/jersey-test-framework-core-2.23.2.jar:\
${m2}/org/glassfish/jersey/containers/jersey-container-servlet-core/2.23.2/jersey-container-servlet-core-2.23.2.jar:\
${m2}/org/glassfish/jersey/core/jersey-server/2.23.2/jersey-server-2.23.2.jar:\
${m2}/org/glassfish/jersey/core/jersey-common/2.23.2/jersey-common-2.23.2.jar:\
${m2}/org/glassfish/jersey/bundles/repackaged/jersey-guava/2.23.2/jersey-guava-2.23.2.jar:\
${m2}/org/glassfish/hk2/osgi-resource-locator/1.0.1/osgi-resource-locator-1.0.1.jar:\
${m2}/javax/ws/rs/javax.ws.rs-api/2.0.1/javax.ws.rs-api-2.0.1.jar:\
${m2}/org/glassfish/jersey/media/jersey-media-jaxb/2.23.2/jersey-media-jaxb-2.23.2.jar:\
${m2}/javax/annotation/javax.annotation-api/1.2/javax.annotation-api-1.2.jar:\
${m2}/org/glassfish/hk2/hk2-api/2.5.0-b05/hk2-api-2.5.0-b05.jar:\
${m2}/org/glassfish/hk2/hk2-utils/2.5.0-b05/hk2-utils-2.5.0-b05.jar:\
${m2}/org/glassfish/hk2/external/aopalliance-repackaged/2.5.0-b05/aopalliance-repackaged-2.5.0-b05.jar:\
${m2}/org/glassfish/hk2/external/javax.inject/2.5.0-b05/javax.inject-2.5.0-b05.jar:\
${m2}/org/glassfish/hk2/hk2-locator/2.5.0-b05/hk2-locator-2.5.0-b05.jar:\
${m2}/javax/validation/validation-api/1.1.0.Final/validation-api-1.1.0.Final.jar:\
${m2}/org/glassfish/jersey/core/jersey-client/2.23.2/jersey-client-2.23.2.jar:\
${m2}/org/ow2/asm/asm-debug-all/5.0.4/asm-debug-all-5.0.4.jar:\
${m2}/org/jboss/aesh/aesh/0.66.14/aesh-0.66.14.jar:\
${m2}/org/fusesource/jansi/jansi/1.11/jansi-1.11.jar:\
${m2}/com/beust/jcommander/1.65/jcommander-1.65.jar:\
${m2}/org/apache/httpcomponents/fluent-hc/4.5.2/fluent-hc-4.5.2.jar:\
${m2}/commons-logging/commons-logging/1.2/commons-logging-1.2.jar:\
${m2}/org/slf4j/slf4j-api/1.7.23/slf4j-api-1.7.23.jar:\
${m2}/javax/javaee-api/7.0/javaee-api-7.0.jar:\
${m2}/com/sun/mail/javax.mail/1.5.0/javax.mail-1.5.0.jar:\
${m2}/javax/activation/activation/1.1/activation-1.1.jar:\
${m2}/junit/junit/4.12/junit-4.12.jar:\
${m2}/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:\
${m2}/org/projectlombok/lombok/1.16.12/lombok-1.16.12.jar:\
${m2}/org/jetbrains/annotations/15.0/annotations-15.0.jar:\
${m2}/org/mockito/mockito-core/2.7.10/mockito-core-2.7.10.jar:\
${m2}/net/bytebuddy/byte-buddy/1.6.5/byte-buddy-1.6.5.jar:\
${m2}/net/bytebuddy/byte-buddy-agent/1.6.5/byte-buddy-agent-1.6.5.jar:\
${m2}/org/assertj/assertj-core/3.6.2/assertj-core-3.6.2.jar:\
${m2}/org/jboss/arquillian/junit/arquillian-junit-container/1.1.12.Final/arquillian-junit-container-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/junit/arquillian-junit-core/1.1.12.Final/arquillian-junit-core-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/test/arquillian-test-api/1.1.12.Final/arquillian-test-api-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/core/arquillian-core-api/1.1.12.Final/arquillian-core-api-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/test/arquillian-test-spi/1.1.12.Final/arquillian-test-spi-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/core/arquillian-core-spi/1.1.12.Final/arquillian-core-spi-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/container/arquillian-container-test-api/1.1.12.Final/arquillian-container-test-api-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/container/arquillian-container-test-spi/1.1.12.Final/arquillian-container-test-spi-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/core/arquillian-core-impl-base/1.1.12.Final/arquillian-core-impl-base-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/test/arquillian-test-impl-base/1.1.12.Final/arquillian-test-impl-base-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/container/arquillian-container-impl-base/1.1.12.Final/arquillian-container-impl-base-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/config/arquillian-config-api/1.1.12.Final/arquillian-config-api-1.1.12.Final.jar:\
${m2}/org/jboss/shrinkwrap/descriptors/shrinkwrap-descriptors-spi/2.0.0-alpha-10/shrinkwrap-descriptors-spi-2.0.0-alpha-10.jar:\
${m2}/org/jboss/arquillian/container/arquillian-container-test-impl-base/1.1.12.Final/arquillian-container-test-impl-base-1.1.12.Final.jar:\
${m2}/org/jboss/shrinkwrap/shrinkwrap-impl-base/1.2.6/shrinkwrap-impl-base-1.2.6.jar:\
${m2}/org/jboss/shrinkwrap/shrinkwrap-spi/1.2.6/shrinkwrap-spi-1.2.6.jar:\
${m2}/org/arquillian/container/arquillian-container-chameleon/1.0.0.Beta1/arquillian-container-chameleon-1.0.0.Beta1.jar:\
${m2}/org/arquillian/container/arquillian-chameleon-container-model/1.0.0.Beta1/arquillian-chameleon-container-model-1.0.0.Beta1.jar:\
${m2}/org/jboss/arquillian/config/arquillian-config-impl-base/1.1.12.Final/arquillian-config-impl-base-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/container/arquillian-container-spi/1.1.12.Final/arquillian-container-spi-1.1.12.Final.jar:\
${m2}/org/jboss/shrinkwrap/descriptors/shrinkwrap-descriptors-api-base/2.0.0-alpha-10/shrinkwrap-descriptors-api-base-2.0.0-alpha-10.jar:\
${m2}/org/jboss/arquillian/protocol/arquillian-protocol-servlet/1.1.12.Final/arquillian-protocol-servlet-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/testenricher/arquillian-testenricher-ejb/1.1.12.Final/arquillian-testenricher-ejb-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/testenricher/arquillian-testenricher-resource/1.1.12.Final/arquillian-testenricher-resource-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/testenricher/arquillian-testenricher-cdi/1.1.12.Final/arquillian-testenricher-cdi-1.1.12.Final.jar:\
${m2}/org/jboss/arquillian/testenricher/arquillian-testenricher-initialcontext/1.1.12.Final/arquillian-testenricher-initialcontext-1.1.12.Final.jar:\
${m2}/org/jboss/shrinkwrap/resolver/shrinkwrap-resolver-spi/2.2.6/shrinkwrap-resolver-spi-2.2.6.jar:\
${m2}/org/jboss/shrinkwrap/resolver/shrinkwrap-resolver-spi-maven/2.2.6/shrinkwrap-resolver-spi-maven-2.2.6.jar:\
${m2}/org/jboss/shrinkwrap/resolver/shrinkwrap-resolver-api-maven-archive/2.2.6/shrinkwrap-resolver-api-maven-archive-2.2.6.jar:\
${m2}/org/jboss/shrinkwrap/resolver/shrinkwrap-resolver-impl-maven/2.2.6/shrinkwrap-resolver-impl-maven-2.2.6.jar:\
${m2}/org/eclipse/aether/aether-api/1.0.0.v20140518/aether-api-1.0.0.v20140518.jar:\
${m2}/org/eclipse/aether/aether-impl/1.0.0.v20140518/aether-impl-1.0.0.v20140518.jar:\
${m2}/org/eclipse/aether/aether-spi/1.0.0.v20140518/aether-spi-1.0.0.v20140518.jar:\
${m2}/org/eclipse/aether/aether-util/1.0.0.v20140518/aether-util-1.0.0.v20140518.jar:\
${m2}/org/eclipse/aether/aether-connector-basic/1.0.0.v20140518/aether-connector-basic-1.0.0.v20140518.jar:\
${m2}/org/eclipse/aether/aether-transport-wagon/1.0.0.v20140518/aether-transport-wagon-1.0.0.v20140518.jar:\
${m2}/commons-lang/commons-lang/2.6/commons-lang-2.6.jar:\
${m2}/org/jsoup/jsoup/1.7.2/jsoup-1.7.2.jar:\
${m2}/commons-io/commons-io/2.2/commons-io-2.2.jar:\
${m2}/org/jboss/shrinkwrap/resolver/shrinkwrap-resolver-impl-maven-archive/2.2.6/shrinkwrap-resolver-impl-maven-archive-2.2.6.jar:\
${m2}/org/jboss/shrinkwrap/resolver/shrinkwrap-resolver-spi-maven-archive/2.2.6/shrinkwrap-resolver-spi-maven-archive-2.2.6.jar:\
${m2}/javax/enterprise/cdi-api/1.0/cdi-api-1.0.jar:\
${m2}/javax/annotation/jsr250-api/1.0/jsr250-api-1.0.jar:\
${m2}/javax/inject/javax.inject/1/javax.inject-1.jar:\
${m2}/org/eclipse/sisu/org.eclipse.sisu.inject/0.3.0.M1/org.eclipse.sisu.inject-0.3.0.M1.jar:\
${m2}/org/arquillian/spacelift/arquillian-spacelift/1.0.0.Alpha9/arquillian-spacelift-1.0.0.Alpha9.jar:\
${m2}/org/arquillian/spacelift/arquillian-spacelift-api/1.0.0.Alpha9/arquillian-spacelift-api-1.0.0.Alpha9.jar:\
${m2}/org/apache/commons/commons-compress/1.8.1/commons-compress-1.8.1.jar \
com.github.t1.deployer.repository.ArtifactoryMockLauncher
