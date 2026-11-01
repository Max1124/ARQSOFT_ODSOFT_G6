// Steps of the three student areas, shared by the three pipelines (Jenkinsfile.local, .staging, .release).
// Loaded from the Jenkinsfile with `load`. The Maven commands come from the environment block of the Jenkinsfile:
//   env.MVN (-Plending), env.MVN_BIB (-Pbibliographic), env.MVN_NOT (-Pnotification).
// Each report is published in `finally`, so a failing test run still leaves its report for the owner.

// ---- Lending policies --------------------------------------------------------------------------------------------

def lendingUnitTests() {
    try {
        sh 'rm -f target/jacoco.exec'
        // Lending tests that start Spring or call the API are tagged "integration" and run in the next stage.
        sh "${env.MVN} verify -DexcludedGroups=integration"
    } finally {
        saveCoverage('target/jacoco.exec', 'lending')
        junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
    }
}

def lendingIntegrationTests() {
    try {
        // Own report folder per stage, so the unit results are not published twice.
        sh 'rm -rf target/surefire-reports'
        // jacoco.exec from the unit stage is kept, so the report published below covers both stages.
        sh "${env.MVN} test -Dgroups=integration"
    } finally {
        saveCoverage('target/jacoco.exec', 'lending')
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
        saveCoverage('target/jacoco-bibliographic.exec', 'bibliographic-unit')
        junit testResults: 'target/surefire-reports-bibliographic/*.xml', allowEmptyResults: true
        publishHTML(target: [reportDir: 'target/site/jacoco-bibliographic', reportFiles: 'index.html',
                             reportName: 'JaCoCo (bibliographic)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
        archiveArtifacts artifacts: 'target/surefire-reports-bibliographic/**,target/site/jacoco-bibliographic/**,target/jacoco-bibliographic.exec', allowEmptyArchive: true
    }
}

def bibliographicStaticAnalysis() {
    try {
        sh "${env.MVN_BIB} compile spotbugs:spotbugs"
        sh 'cp target/spotbugsXml.xml target/spotbugsXml-bibliographic.xml'
    } finally {
        archiveArtifacts artifacts: 'target/spotbugsXml-bibliographic.xml', allowEmptyArchive: true
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
        saveCoverage('target/jacoco.exec', 'notification')
        // This profile shares the surefire directory with the lending stage, so only its classes are published.
        junit testResults: 'target/surefire-reports/TEST-*Notification*.xml', allowEmptyResults: true
        publishHTML(target: [reportDir: 'target/site/jacoco-notification', reportFiles: 'index.html',
                             reportName: 'JaCoCo (notification)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
        archiveArtifacts artifacts: 'target/surefire-reports/TEST-*Notification*.xml,target/site/jacoco-notification/**', allowEmptyArchive: true
    }
}

def notificationIntegrationTests() {
    try {
        // Fixed ports (Mailpit, Mockoon), and the pipelines of one Jenkins can run at the same time.
        lock('notification-its') {
            sh 'bash jenkins/run-notification-its.sh'
        }
    } finally {
        saveCoverage('target/jacoco-notification-it.exec', 'notification-it')
        junit testResults: 'target/surefire-reports-notification-it/TEST-*IT.xml', allowEmptyResults: true
        archiveArtifacts artifacts: 'target/surefire-reports-notification-it/TEST-*IT.xml,target/notification-its/**', allowEmptyArchive: true
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

def unitTests() {
    try {
        sh './mvnw -B clean test -Dtest="*Test" -Dsurefire.reportsDirectory=target/surefire-reports-unit'
    } finally {
        saveCoverage('target/jacoco.exec', 'unit')
        junit testResults: 'target/surefire-reports-unit/*.xml', allowEmptyResults: true
    }
}

def existingIntegrationTests() {
    try {
        sh 'rm -f target/jacoco.exec'
        sh './mvnw -B test -Dtest="*IT,!**/bibliographicmanagement/**,!*Notification*IT,!ScheduledNotificationCheckIT,!LendingSystemIT" -Dsurefire.reportsDirectory=target/surefire-reports-existing-it'
    } finally {
        saveCoverage('target/jacoco.exec', 'existing-it')
        junit testResults: 'target/surefire-reports-existing-it/*.xml', allowEmptyResults: true
    }
}

def bibliographicIntegrationTests() {
    try {
        sh 'bash jenkins/run-bibliographic-its.sh'
    } finally {
        saveCoverage('target/jacoco-bibliographic.exec', 'bibliographic-it')
        junit testResults: 'target/surefire-reports-bibliographic-it/*.xml', allowEmptyResults: true
        publishHTML(target: [reportDir: 'target/site/jacoco-bibliographic', reportFiles: 'index.html',
                             reportName: 'JaCoCo (bibliographic)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
        archiveArtifacts artifacts: 'target/surefire-reports-bibliographic-it/**,target/site/jacoco-bibliographic/**,target/jacoco-bibliographic.exec', allowEmptyArchive: true
    }
}

// Save each suite before another stage resets the default JaCoCo file.
def saveCoverage(source, name) {
    if (fileExists(source)) {
        sh "mkdir -p target/coverage-data && cp '${source}' 'target/coverage-data/${name}.exec'"
    }
}

def globalCoverage() {
    try {
        sh 'bash jenkins/publish-global-coverage.sh'
    } finally {
        publishHTML(target: [reportDir: 'target/site/jacoco-global', reportFiles: 'index.html',
                             reportName: 'JaCoCo (global)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
        archiveArtifacts artifacts: 'target/site/jacoco-global/**,target/jacoco-global.exec,target/coverage-data/*.exec', allowEmptyArchive: true
    }
}

def notificationStaticAnalysis() {
    try {
        sh "${env.MVN_NOT} spotbugs:spotbugs"
        sh 'cp target/spotbugsXml.xml target/spotbugsXml-notification.xml'
    } finally {
        archiveArtifacts artifacts: 'target/spotbugsXml-notification.xml', allowEmptyArchive: true
    }
}

return this
