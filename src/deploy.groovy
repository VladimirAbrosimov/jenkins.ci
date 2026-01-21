@Library('abrosimov.jenkins') _

import ru.abrosimov.jenkins.ci.context.Application
import ru.abrosimov.jenkins.ci.context.PipelineContext
import ru.abrosimov.jenkins.ci.stages.BuildAndPublish
import ru.abrosimov.jenkins.ci.stages.BuildDockerImage
import ru.abrosimov.jenkins.ci.stages.Checkout
import ru.abrosimov.jenkins.ci.stages.GetVersion
import ru.abrosimov.jenkins.ci.stages.IncrementVersion
import ru.abrosimov.jenkins.ci.stages.PushToRegistry
import ru.abrosimov.jenkins.core.Logger

PipelineContext pipelineContext
List<String> applicationDescriptors = [
        "src/apps/Defi.groovy"
]
boolean skipBuild = true

pipeline {
    agent any

    stages {
        stage("Init pipeline") {
            when {
                expression { params.APPLICATION_DESCRIPTOR }
            }
            steps {
                script {
                    Logger.startStage(this)

                    pipelineContext = load "src/PipelineContextImpl.groovy"
                    Application application = load params.APPLICATION_DESCRIPTOR
                    pipelineContext.setApplication(application)
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

                    Checkout checkout = new Checkout(this)
                    checkout.call(pipelineContext)

                    Logger.endStage(this)
                }
            }
        }

        stage("Get current version") {
            when {
                expression { !skipBuild }
            }
            steps {
                script {
                    Logger.startStage(this)

                    GetVersion getVersion = new GetVersion(this)
                    String applicationVersion = getVersion.call()
                    pipelineContext.application.setVersion(applicationVersion)
                    currentBuild.description = "Version: ${applicationVersion}"

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

        stage("Build Docker image") {
            when {
                expression { !skipBuild }
            }
            steps {
                script {
                    Logger.startStage(this)

                    BuildDockerImage buildDockerImage = new BuildDockerImage(this)
                    buildDockerImage.call(pipelineContext)

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

                    PushToRegistry pushToRegistry = new PushToRegistry(this)
                    pushToRegistry.call(pipelineContext)

                    Logger.endStage(this)
                }
            }
        }

        stage("Build and publish") {
            when {
                expression { !skipBuild }
            }
            steps {
                script {
                    Logger.startStage(this)

                    BuildAndPublish buildAndPublish = new BuildAndPublish(this)
                    buildAndPublish.call()

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

                    IncrementVersion incrementVersion = new IncrementVersion(this)
                    incrementVersion.call(pipelineContext)

                    Logger.endStage(this)
                }
            }
        }
    }
}
