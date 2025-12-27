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

pipeline {
    agent any

    stages {
        stage("Checkout") {
            steps {
                script {
                    logStartStage()

                    sshagent(credentials: ["SSH_KEY_GITHUB"]) {
                        git(
                            url: "git@github.com:vabrosimov/defi.git",
                            branch: "master"
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

                    String currentVersion = sh(
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
                sh "./gradlew clean build"
            }
        }

        stage("Build Docker Image") {
            steps {
                sh """
                  docker build \
                    -t ${IMAGE_NAME}:${APP_VERSION} \
                    -t ${IMAGE_NAME}:latest \
                    .
                """
            }
        }

        stage("Push to Docker Hub") {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: "DOCKERHUB_CREDENTIALS",
                        usernameVariable: "DOCKER_USER",
                        passwordVariable: "DOCKER_PASS"
                    )
                ]) {
                    sh """
                      echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                      docker push ${IMAGE_NAME}:${APP_VERSION}
                      docker push ${IMAGE_NAME}:latest
                    """
                }
            }
        }
    }
}
