pipeline {
    agent any

    stages {

        stage('Build') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Tests') {
            steps {
                bat 'mvn test'
            }
        }
    }
}
