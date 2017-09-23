FROM java:8-jre
COPY target/artifactory-mock.jar /opt/dropwizard/
EXPOSE 8081
WORKDIR /opt/dropwizard
RUN mkdir -p /root/.m2
VOLUME /root/.m2
ENTRYPOINT ["java", "-jar", "artifactory-mock.jar"]
