@Library('abrosimov.jenkins') _

import ru.abrosimov.jenkins.ci.context.Application
import ru.abrosimov.jenkins.ci.stages.GetAndIncrementVersion
import ru.abrosimov.jenkins.core.Logger

String currentVersion
Application application
List<String> applicationDescriptors = [
        "src/apps/Defi.groovy"
]
boolean skipBuild = true

pipeline {
    agent any

    environment {
        REGISTRY = "95.174.94.249:8082/repository/registry/"
    }

    stages {
        stage("Init pipeline") {
            when {
                expression { params.APPLICATION_DESCRIPTOR }
            }
            steps {
                script {
                    Logger.startStage(this)

                    application = load params.APPLICATION_DESCRIPTOR
                    skipBuild = false

                    Logger.endStage(this)
                }
            }
        }

        stage("Configure pipeline") {
            steps {
                script {
                    Logger.startStage(this)

                    properties([parameters([choice(
                            name: "APPLICATION_DESCRIPTOR",
                            choices: applicationDescriptors,
                            description: 'Application to build'
                    )])])

                    Logger.endStage(this)
                }
            }
        }

        stage("Checkout") {
            when {
                expression { !skipBuild }
            }
            steps {
                script {
                    Logger.startStage(this)

                    sshagent(credentials: ["SSH_KEY_GITHUB"]) {
                        git(
                            url: "${application.git}",
                            branch: "master",
                            credentialsId: 'SSH_KEY_GITHUB'
                        )
                    }

                    Logger.endStage(this)
                }
            }
        }

        stage("Build & Publish") {
            when {
                expression { !skipBuild }
            }
            steps {
                script {
                    Logger.startStage(this)

                    withCredentials([
                        usernamePassword(
                            credentialsId: "NEXUS_CREDENTIALS",
                            usernameVariable: "NEXUS_USER",
                            passwordVariable: "NEXUS_PASSWORD"
                        )
                    ]) {
                        sh '''
                          ./gradlew clean publish -PNEXUS_USER=$NEXUS_USER -PNEXUS_PASSWORD=$NEXUS_PASSWORD
                        '''
                    }

                    Logger.endStage(this)
                }
            }
        }

        stage("Increment version") {
            when {
                expression { !skipBuild }
            }
            steps {
                script {
                    Logger.startStage(this)

                    GetAndIncrementVersion getAndIncrementVersion = new GetAndIncrementVersion(this)

                    currentVersion = getAndIncrementVersion.call()

                    Logger.endStage(this)
                }
            }
        }

        stage("Build JAR") {
            when {
                expression { !skipBuild }
            }
            steps {
                script {
                    Logger.startStage(this)

                    sh "./gradlew clean build"

                    Logger.endStage(this)
                }
            }
        }

        stage("Build Docker Image") {
            when {
                expression { !skipBuild }
            }
            steps {
                script {
                    Logger.startStage(this)

                    sh """
                    docker build \
                    --platform=linux/amd64 \
                    -t ${REGISTRY}${application.image}:${currentVersion} \
                    -t ${REGISTRY}${application.image}:latest \
                    .
                    """

                    Logger.endStage(this)
                }
            }
        }

        stage("Push to registry") {
            when {
                expression { !skipBuild }
            }
            steps {
                script {
                    Logger.startStage(this)

                    def imageWithVersion = "${REGISTRY}${application.image}:${currentVersion}"
                    def imageLatest      = "${REGISTRY}${application.image}:latest"

                    withCredentials([
                        usernamePassword(
                            credentialsId: "NEXUS_CREDENTIALS",
                            usernameVariable: "NEXUS_USER",
                            passwordVariable: "NEXUS_PASSWORD"
                        )
                    ]) {
                        withEnv([
                            "IMAGE_WITH_VERSION=${imageWithVersion}",
                            "IMAGE_LATEST=${imageLatest}"
                        ]) {
                            sh '''
                                echo "$NEXUS_PASSWORD" | docker login "$REGISTRY" \
                                  -u "$NEXUS_USER" \
                                  --password-stdin

                                docker push "$IMAGE_WITH_VERSION"
                                docker push "$IMAGE_LATEST"
                            '''
                        }
                    }

                    Logger.endStage(this)
                }
            }
        }
    }
}
