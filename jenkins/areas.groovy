// Steps of the three student areas, shared by the three pipelines (Jenkinsfile.local, .staging, .release).
// Loaded from the Jenkinsfile with `load`. The Maven commands come from the environment block of the Jenkinsfile:
//   env.MVN (-Plending), env.MVN_BIB (-Pbibliographic), env.MVN_NOT (-Pnotification).
// Each report is published in `finally`, so a failing test run still leaves its report for the owner.

// ---- Lending policies --------------------------------------------------------------------------------------------

def lendingTests() {
    try {
        sh "${env.MVN} verify"
    } finally {
        junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
        publishHTML(target: [reportDir: 'target/site/jacoco', reportFiles: 'index.html',
                             reportName: 'JaCoCo (lending)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
    }
}

def lendingStaticAnalysis() {
    try {
        sh "${env.MVN} spotbugs:spotbugs"
    } finally {
        archiveArtifacts artifacts: 'target/spotbugsXml.xml', allowEmptyArchive: true
    }
}

def lendingMutation() {
    try {
        sh "${env.MVN} test-compile org.pitest:pitest-maven:mutationCoverage"
    } finally {
        publishHTML(target: [reportDir: 'target/pit-reports', reportFiles: 'index.html',
                             reportName: 'PIT (lending)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
    }
}

// ---- Bibliographic sources ---------------------------------------------------------------------------------------

def bibliographicTests() {
    try {
        // Do not clean: preserve the packaged JAR and the lending reports.
        sh "${env.MVN_BIB} test"
    } finally {
        junit testResults: 'target/surefire-reports-bibliographic/*.xml', allowEmptyResults: true
        publishHTML(target: [reportDir: 'target/site/jacoco-bibliographic', reportFiles: 'index.html',
                             reportName: 'JaCoCo (bibliographic)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
        archiveArtifacts artifacts: 'target/surefire-reports-bibliographic/**,target/site/jacoco-bibliographic/**,target/jacoco-bibliographic.exec', allowEmptyArchive: true
    }
}

def bibliographicMutation() {
    try {
        sh "${env.MVN_BIB} test-compile org.pitest:pitest-maven:mutationCoverage"
    } finally {
        publishHTML(target: [reportDir: 'target/pit-reports-bibliographic', reportFiles: 'index.html',
                             reportName: 'PIT (bibliographic)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
        archiveArtifacts artifacts: 'target/pit-reports-bibliographic/**', allowEmptyArchive: true
    }
}

// ---- Reader notifications ----------------------------------------------------------------------------------------

def notificationTests() {
    try {
        // The *IT classes are not selected: 0 real messages. The lending coverage is already published;
        // removing its data keeps this report to the notification tests.
        sh 'rm -f target/jacoco.exec'
        sh "${env.MVN_NOT} test"
        sh 'rm -rf target/site/jacoco-notification && cp -r target/site/jacoco target/site/jacoco-notification'
    } finally {
        // This profile shares the surefire directory with the lending stage, so only its classes are published.
        junit testResults: 'target/surefire-reports/TEST-*Notification*.xml', allowEmptyResults: true
        publishHTML(target: [reportDir: 'target/site/jacoco-notification', reportFiles: 'index.html',
                             reportName: 'JaCoCo (notification)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
        archiveArtifacts artifacts: 'target/surefire-reports/TEST-*Notification*.xml,target/site/jacoco-notification/**', allowEmptyArchive: true
    }
}

def notificationMutation() {
    try {
        sh "${env.MVN_NOT} test-compile org.pitest:pitest-maven:mutationCoverage"
    } finally {
        publishHTML(target: [reportDir: 'target/pit-reports-notification', reportFiles: 'index.html',
                             reportName: 'PIT (notification)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
        archiveArtifacts artifacts: 'target/pit-reports-notification/**', allowEmptyArchive: true
    }
}

return this
