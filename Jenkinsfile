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

        stage("Build") {
            steps {
                script {
                    logStartStage()

                    sh "./gradlew clean build"

                    logEndStage()
                }
            }
        }
    }
}
