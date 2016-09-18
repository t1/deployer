node {
    stage 'Checkout'
    checkout scm

    stage 'Package'
    def mvnHome = tool 'M3'
    sh "${mvnHome}/bin/mvn clean package"
    step([$class: 'ArtifactArchiver', artifacts: '**/target/*.war', fingerprint: true])
    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

    if (false) {
        stage 'Integration Test'
        try {
            sh "${mvnHome}/bin/mvn failsafe:integration-test failsafe:verify -DtrimStackTrace=false -DuseFile=false"
        } catch (err) {
            echo "error during integration-test: ${err}"
            // currentBuild.result = 'UNSTABLE'
        }
        step([$class: 'JUnitResultArchiver', testResults: '**/target/failsafe-reports/TEST-*.xml'])
    }
}
