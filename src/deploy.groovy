@Library('abrosimov.jenkins') _

import ru.abrosimov.jenkins.utils.Logger
import apps.Application

String currentVersion
Application application

pipeline {
    agent any

    environment {
        GIT = "git@github.com:vabrosimov/defi.git"
        IMAGE_NAME = "vabrosimov/defi"
        REGISTRY = "95.174.94.249:8082/repository/registry/"
    }

    stages {
        stage("Checkout") {
            steps {
                script {
                    Logger.startStage(this)

                    sshagent(credentials: ["SSH_KEY_GITHUB"]) {
                        git(
                            url: "${GIT}",
                            branch: "master",
                            credentialsId: 'SSH_KEY_GITHUB'
                        )
                    }

                    Logger.endStage(this)
                }
            }
        }

        stage("Build & Publish") {
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
            steps {
                script {
                    Logger.startStage(this)

                    String versionFile = "version.properties"

                    currentVersion = sh(
                        script: "grep '^version=' ${versionFile} | cut -d'=' -f2",
                        returnStdout: true
                    ).trim()
                    echo "Current version: ${currentVersion}"

                    def parts = currentVersion.tokenize('-')
                    def nextVersion = "${parts[0]}-${parts[1].toInteger() + 1}"

                    writeFile(
                        file: versionFile,
                        text: "version=${nextVersion}\n"
                    )

                    echo "Next version set to: ${nextVersion}"

                    sshagent(credentials: ["SSH_KEY_GITHUB"]) {
                        sh """
                        mkdir -p -m 700 ~/.ssh
                        ssh-keyscan -H github.com >> ~/.ssh/known_hosts
                        chmod 600 ~/.ssh/known_hosts

                        git config user.name "Jenkins CI"
                        git config user.email "ci@jenkins.local"
                        git add ${versionFile}
                        git commit -m "chore: bump version to ${nextVersion}"
                        git push origin master
                        """
                    }

                    Logger.endStage(this)
                }
            }
        }

        stage("Build JAR") {
            steps {
                script {
                    Logger.startStage(this)

                    sh "./gradlew clean build"

                    Logger.endStage(this)
                }
            }
        }

        stage("Build Docker Image") {
            steps {
                script {
                    Logger.startStage(this)

                    sh """
                    docker build \
                    --platform=linux/amd64 \
                    -t ${REGISTRY}${IMAGE_NAME}:${currentVersion} \
                    -t ${REGISTRY}${IMAGE_NAME}:latest \
                    .
                    """

                    Logger.endStage(this)
                }
            }
        }

        stage("Push to registry") {
            steps {
                script {
                    Logger.startStage(this)

                    def imageWithVersion = "${REGISTRY}${IMAGE_NAME}:${currentVersion}"
                    def imageLatest      = "${REGISTRY}${IMAGE_NAME}:latest"

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
