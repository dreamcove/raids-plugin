pipeline {
    agent {
        docker {
            image 'maven:3.6.3-jdk-11'
        }
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '5'))
        timeout(time: 1, unit: 'HOURS')
    }
    environment {
        PROJECT = 'minecraft:plugins:raids'
    }
    stages {
        stage ('Initialize') {
            steps {
                withCredentials([file(credentialsId: 'MAVEN_SETTINGS_XML', variable: 'mavensettings')]) {
                    sh "cp \$mavensettings /root/.m2/settings.xml"
                }
            }
        }
        stage ('Dependencies') {
            steps {
                sh 'mvn clean dependency:resolve'
            }
        }
        stage ('Compile') {
            steps {
                sh 'mvn compile'
            }
        }
        stage ('Test') {
            steps {
                sh 'mvn test'
            }
        }
        stage ('Package') {
            steps {
                sh 'mvn package'
            }
        }
        stage('Code Analysis') {
            environment {
                scannerHome = tool 'primary'
            }
            steps {
                withSonarQubeEnv('Dreamcove') {
                    sh "${scannerHome}/bin/sonar-scanner -D sonar.links.scm=${env.GIT_URL} -D sonar.projectKey=${env.PROJECT}:`echo ${env.BRANCH_NAME} | tr \\/ _`"
                    sh 'sleep 10'
                }
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage ('Snapshot Deploy') {
            when { branch 'develop' }
            steps {
                sh 'mvn deploy'
            }
        }
    }
}