pipeline {
    agent any

    stages {

        stage('Build') {
            steps {
                bat 'mvnw.cmd clean package -DskipTests'
            }
        }

        stage('Tests') {
            steps {
                bat 'mvnw.cmd test'
            }
        }
    }
}