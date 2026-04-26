@Library('platform-lib') _

import platform.GitHub

pipeline {
    agent any

    parameters {
        string(
            name: 'REPO_NAME',
            description: 'Repository name'
        )
    }

    stages {

        stage('Validate') {
            steps {
                script {
                    if (!params.REPO_NAME?.trim()) {
                        error "REPO_NAME is required"
                    }

                    echo "Creating jobs for: ${params.REPO_NAME}"
                }
            }
        }

        stage('Run Job DSL') {
            steps {
                script {

                    def repo = params.REPO_NAME
                    def repoUrl = GitHub.repoUrl(repo)

                    jobDsl(
                        targets: 'jobs/createJobs.groovy',
                        removedJobAction: 'IGNORE',
                        removedViewAction: 'IGNORE',
                        additionalParameters: [
                            REPO_NAME: repo,
                            REPO_URL : repoUrl
                        ]
                    )
                }
            }
        }

        stage('Trigger AutoGen') {
            steps {
                build job: "${params.REPO_NAME}/AutoGen",
                    parameters: [
                        string(name: 'BRANCH', value: 'main'),
                        string(name: 'ENV', value: 'dev')
                    ]
            }
        }
    }
}