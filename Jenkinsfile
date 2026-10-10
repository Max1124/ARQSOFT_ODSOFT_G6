pipeline {
    agent any

    stages {
        stage('Check Java') {
            steps {
                sh 'java -version'
                sh 'javac -version'
                sh 'echo $JAVA_HOME'
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Tests') {
            steps {
                sh './mvnw test'
            }
        }
    }

}
