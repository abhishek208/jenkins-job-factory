// ===============================
// seed.groovy
// ===============================

@Library('platform-lib') _

import platform.GitHub

pipeline {
    agent any

    parameters {
        string(
            name: 'REPO_NAME',
            description: 'GitHub repository name (example: sample-service)'
        )
    }

    stages {

        stage('Validate Input') {
            steps {
                script {
                    if (!params.REPO_NAME?.trim()) {
                        error "REPO_NAME is required"
                    }

                    echo "Bootstrapping repository: ${params.REPO_NAME}"
                }
            }
        }

        stage('Create Folder + AutoGen Job') {
            steps {
                script {

                    def repo = params.REPO_NAME
                    def repoUrl = GitHub.repoUrl(repo)

                    def dsl = """
folder('${repo}')

pipelineJob('${repo}/AutoGen') {
    description('AutoGen job for ${repo}')

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('${repoUrl}')
                    }
                    branches('main')
                }
            }
            scriptPath('autogen.groovy')
        }
    }
}
"""

                    jobDsl(
                        scriptText: dsl,
                        removedJobAction: 'IGNORE',
                        removedViewAction: 'IGNORE'
                    )
                }
            }
        }

        stage('Trigger First AutoGen Run') {
            steps {
                script {
                    build job: "${params.REPO_NAME}/AutoGen",
                        parameters: [
                            string(
                                name: 'BRANCH',
                                value: 'main'
                            ),
                            string(
                                name: 'ENV',
                                value: 'dev'
                            )
                        ]
                }
            }
        }
    }
}