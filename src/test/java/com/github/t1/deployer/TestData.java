package com.github.t1.deployer;

import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Expressions.VariableName;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;

public class TestData {
    public static final VariableName VERSION = new VariableName("version");

    public static final GroupId ORG_JOLOKIA = new GroupId("org.jolokia");
    public static final ArtifactId JOLOKIA_WAR = new ArtifactId("jolokia-war");
    public static final Version VERSION_1_3_3 = v("1.3.3");

    public static final Checksum JOLOKIA_131_CHECKSUM = checksumFor(ORG_JOLOKIA, JOLOKIA_WAR, war, v("1.3.1"));
    public static final Checksum JOLOKIA_132_CHECKSUM = checksumFor(ORG_JOLOKIA, JOLOKIA_WAR, war, v("1.3.2"));
    public static final Checksum JOLOKIA_133_CHECKSUM = checksumFor(ORG_JOLOKIA, JOLOKIA_WAR, war, VERSION_1_3_3);
    public static final Checksum JOLOKIA_134_CHECKSUM = checksumFor(ORG_JOLOKIA, JOLOKIA_WAR, war, v("1.3.4"));
    public static final Checksum JOLOKIA_133_POM_CHECKSUM = checksumFor(ORG_JOLOKIA, JOLOKIA_WAR, pom, VERSION_1_3_3);
    public static final Checksum JOLOKIA_134_SNAPSHOT_CHECKSUM
            = dummyWar(ORG_JOLOKIA, JOLOKIA_WAR, v("1.3.4-SNAPSHOT"));

    public static final Checksum POSTGRESQL_9_4_1207_CHECKSUM = checksumFor
            (new GroupId("org.postgresql"), new ArtifactId("postgresql"), jar, v("9.4.1207"));

    private static Version v(String value) { return new Version(value); }
}
