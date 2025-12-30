def logStartStage() {
    ansiColor('xterm') {
        echo """
        \u001B[34m══════════════════════════════════════════════\u001B[0m
        \u001B[36m▶▶▶ START STAGE: ${STAGE_NAME}\u001B[0m
        \u001B[34m══════════════════════════════════════════════\u001B[0m
        """.stripIndent()
    }
}

def logEndStage() {
    ansiColor('xterm') {
        echo """
        \u001B[32m✔✔✔ END STAGE: ${STAGE_NAME}\u001B[0m
        """.stripIndent()
    }
}

String currentVersion

pipeline {
    agent any

    environment {
        IMAGE_NAME = "vabrosimov/defi"
        REGISTRY = "nexus:8082/repository/registry/"
    }

    stages {
        stage("Checkout") {
            steps {
                script {
                    logStartStage()

                    sshagent(credentials: ["SSH_KEY_GITHUB"]) {
                        git(
                            url: "git@github.com:vabrosimov/defi.git",
                            branch: "master",
                            credentialsId: 'SSH_KEY_GITHUB'
                        )
                    }

                    logEndStage()
                }
            }
        }

        stage("Build & Publish") {
            steps {
                script {
                    logStartStage()

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

                    logEndStage()
                }
            }
        }

        stage("Increment version") {
            steps {
                script {
                    logStartStage()

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
                        mkdir -p ~/.ssh
                        ssh-keyscan github.com >> ~/.ssh/known_hosts

                        git config user.name "Jenkins CI"
                        git config user.email "ci@jenkins.local"
                        git add ${versionFile}
                        git commit -m "chore: bump version to ${nextVersion}"
                        git push origin master
                        """
                    }

                    logEndStage()
                }
            }
        }

        stage("Build JAR") {
            steps {
                script {
                    logStartStage()

                    sh "./gradlew clean build"

                    logEndStage()
                }
            }
        }

        stage("Build Docker Image") {
            steps {
                script {
                    logStartStage()

                    sh """
                    docker build \
                    --platform=linux/amd64 \
                    -t ${REGISTRY}${IMAGE_NAME}:${currentVersion} \
                    -t ${REGISTRY}${IMAGE_NAME}:latest \
                    .
                    """

                    logEndStage()
                }
            }
        }

        stage("Push to registry") {
            steps {
                script {
                    logStartStage()

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

                    logEndStage()
                }
            }
        }
    }
}
