def logStartStage() {
    ansiColor('xterm') {
        echo """
        \u001B[34m══════════════════════════════════════════════\u001B[0m
        \u001B[36m▶▶▶ START STAGE: ${env.STAGE_NAME}\u001B[0m
        \u001B[34m══════════════════════════════════════════════\u001B[0m
        """.stripIndent()
    }
}

def logEndStage() {
    ansiColor('xterm') {
        echo """
        \u001B[32m✔✔✔ END STAGE: ${env.STAGE_NAME}\u001B[0m
        """.stripIndent()
    }
}

String currentVersion

pipeline {
    agent any

    environment {
        IMAGE_NAME = "vabrosimov/defi"
        REGISTRY = "http://localhost:8081/repository/registry/"
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

                    sh """
                    ./gradlew clean publish
                    """

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
                      docker buildx build \
                        --platform linux/amd64 \
                        -t ${IMAGE_NAME}:${currentVersion} \
                        -t ${IMAGE_NAME}:latest \
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

                    withCredentials([
                        usernamePassword(
                            credentialsId: "NEXUS_CREDENTIALS",
                            usernameVariable: "NEXUS_USER",
                            passwordVariable: "NEXUS_PASSWORD"
                        )
                    ]) {
                        sh """
                          echo "$NEXUS_PASSWORD" | docker login -u "$DOCKER_USER" --password-stdin
                          docker push ${REGISTRY}/${IMAGE_NAME}:${currentVersion}
                          docker push ${REGISTRY}/${IMAGE_NAME}:latest
                        """
                    }

                    logEndStage()
                }
            }
        }
    }
}
