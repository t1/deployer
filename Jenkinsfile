node {
    stage 'Checkout'
    checkout scm

    stage 'Package'
    def mvnHome = tool 'M3'
    sh "${mvnHome}/bin/mvn -Dmaven.test.failure.ignore clean package"
    step([$class: 'ArtifactArchiver', artifacts: '**/target/*.war', fingerprint: true])
    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

    stage 'Integration Test'
    sh "${mvnHome}/bin/mvn -Dmaven.test.failure.ignore integration-test"
}
