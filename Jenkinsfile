pipeline {
    agent any

    stages {
        stage("Checkout") {
            steps {
                sshagent(credentials: ["SSH_KEY_GITHUB"]) {
                    git(
                        url: "git@github.com:vabrosimov/defi.git",
                        branch: "master"
                    )
                }
            }
        }

        stage("Build") {
            steps {
                sh """
                  ./gradlew clean build
                """
            }
        }
    }
}
