pipeline {
    agent {
        docker {
            image 'maven:3.6.3-jdk-11'
            args '-v $HOME/.m2:/root/.m2'
        }
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '5'))
        timeout(time: 1, unit: 'HOURS')
    }
    environment {
        PROJECT = 'minecraft:plugins:raids'
        DISCORD_WEBHOOK = credentials("DISCORD_WEBHOOK")
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
                    sh "${scannerHome}/bin/sonar-scanner -D sonar.java.binaries=target/classes -D sonar.links.scm=${env.GIT_URL} -D sonar.test.exclusions=**/test/** -D sonar.exclusions=**/Test*.java,**/impl/**,**/RaidsPlugin.java -D sonar.projectKey=${env.PROJECT}:`echo ${env.BRANCH_NAME} | tr \\/ _`"
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
    post {
        success {
            discordSend description: "Build ${env.JOB_NAME} (#${env.BUILD_NUMBER}) on ${env.BRANCH_NAME} completed successfully", link: env.BUILD_URL, result: currentBuild.currentResult, title: "${env.JOB_NAME} on branch ${env.BRANCH_NAME}", webhookURL: env.DISCORD_WEBHOOK
        }
        failure {
            discordSend description: "Build ${env.JOB_NAME} (#${env.BUILD_NUMBER}) on ${env.BRANCH_NAME} failed", link: env.BUILD_URL, result: currentBuild.currentResult, title: "${env.JOB_NAME} on branch ${env.BRANCH_NAME}", webhookURL: env.DISCORD_WEBHOOK
        }
    }
}
