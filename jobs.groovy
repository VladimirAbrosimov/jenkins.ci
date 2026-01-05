folder('ci') {
}

pipelineJob('ci/build_distrib') {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('git@github.com:vabrosimov/jenkins.ci.git')
                        credentials('SSH_KEY_GITHUB')
                    }
                    branch('develop')
                }
            }
            scriptPath('Jenkinsfile')
        }
    }
}

folder('cd') {
}

pipelineJob('cd/build_distrib') {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('git@github.com:vabrosimov/jenkins.cd.git')
                        credentials('SSH_KEY_GITHUB')
                    }
                    branch('develop')
                }
            }
            scriptPath('Jenkinsfile')
        }
    }
}