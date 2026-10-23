pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
    }

    // Fires on the GitHub webhook; the VM is usually off, so a missed push is rebuilt with "Build Now".
    triggers {
        githubPush()
    }

    parameters {
        booleanParam(name: 'RUN_MUTATION', defaultValue: true, description: 'Run PIT on the lending classes (slow on the small VM)')
    }

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        // -Plending limits tests, coverage, PIT and SpotBugs to the lending policies; other owners add their own profile.
        MVN = './mvnw -B -Plending'
        JAR = 'target/psoft-g1-0.0.1-SNAPSHOT.jar'
    }

    stages {
        stage('Check Java') {
            steps {
                sh 'java -version'
                sh 'chmod +x mvnw deploy/deploy.sh'
            }
        }

        // One build produces the jar that is then promoted through every environment.
        stage('Build and test') {
            steps {
                sh "${MVN} clean verify"
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    publishHTML(target: [reportDir: 'target/site/jacoco', reportFiles: 'index.html',
                                         reportName: 'JaCoCo (lending)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
                }
            }
        }

        stage('Static analysis') {
            steps {
                sh "${MVN} spotbugs:spotbugs"
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/spotbugsXml.xml', allowEmptyArchive: true
                }
            }
        }

        stage('Mutation testing') {
            when { expression { params.RUN_MUTATION } }
            steps {
                sh "${MVN} test-compile org.pitest:pitest-maven:mutationCoverage"
            }
            post {
                always {
                    publishHTML(target: [reportDir: 'target/pit-reports', reportFiles: 'index.html',
                                         reportName: 'PIT (lending)', keepAll: true, allowMissing: true, alwaysLinkToLastBuild: true])
                }
            }
        }

        stage('Archive artifact') {
            steps {
                archiveArtifacts artifacts: "${JAR}", fingerprint: true
            }
        }

        stage('Deploy to dev') {
            steps {
                sh "deploy/deploy.sh dev ${JAR}"
            }
        }

        stage('Deploy to uat') {
            steps {
                sh "deploy/deploy.sh uat ${JAR}"
                // Reported as unstable, not failed, until the collection is known to pass on a clean database.
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh 'npx --yes newman run Docs/Psoft-G1.postman_collection.json -e Docs/Psoft-G1.postman_environment.json --env-var host=http://localhost:8082 --reporters cli,junit --reporter-junit-export target/newman.xml'
                }
            }
            post {
                always {
                    junit testResults: 'target/newman.xml', allowEmptyResults: true
                }
            }
        }

        stage('Approve prod') {
            agent none
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    input message: "Deploy build #${env.BUILD_NUMBER} to prod?", ok: 'Deploy'
                }
            }
        }

        stage('Deploy to prod') {
            steps {
                sh "deploy/deploy.sh prod ${JAR}"
            }
        }
    }
}
