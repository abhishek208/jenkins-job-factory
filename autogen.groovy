// ===============================
// autogen.groovy
// ===============================

@Library('platform-lib') _

import platform.GitHub

pipeline {
    agent any

    parameters {
        string(
            name: 'BRANCH',
            defaultValue: 'main',
            description: 'Branch to checkout'
        )

        choice(
            name: 'ENV',
            choices: ['dev', 'workflow'],
            description: 'Environment for job generation'
        )
    }

    environment {
        // Example:
        // JOB_NAME = sample-service/AutoGen
        // REPO_NAME = sample-service
        REPO_NAME = "${env.JOB_NAME.split('/')[0]}"
        REPO_URL  = "${GitHub.repoUrl(env.JOB_NAME.split('/')[0])}"
    }

    stages {

        stage('Init') {
            steps {
                script {
                    echo "Repo Name : ${env.REPO_NAME}"
                    echo "Repo URL  : ${env.REPO_URL}"
                    echo "Branch    : ${params.BRANCH}"
                    echo "ENV       : ${params.ENV}"
                }
            }
        }

        stage('Checkout Repository') {
            steps {
                git(
                    branch: "${params.BRANCH}",
                    url: "${env.REPO_URL}"
                )
            }
        }

        stage('Validate workflow.yaml') {
            steps {
                script {
                    def workflowFile = "automation/workflow.yaml"

                    if (!fileExists(workflowFile)) {
                        error "workflow.yaml not found at ${workflowFile}"
                    }

                    echo "workflow.yaml found"
                }
            }
        }

        stage('Generate Jobs') {
            steps {
                script {

                    def config = readYaml(
                        file: 'automation/workflow.yaml'
                    )

                    for (job in config.jobs) {

                        // Skip jobs not matching selected ENV
                        if (job.env && job.env != params.ENV) {
                            echo "Skipping ${job.name} for ENV=${params.ENV}"
                            continue
                        }

                        def jobFile = "automation/${job.file}"

                        if (!fileExists(jobFile)) {
                            echo "Missing file: ${jobFile}"
                            continue
                        }

                        def pipelineScript = readFile(jobFile)

                        jobDsl(
                            targets: 'jobs/generatedJobs.groovy',
                            removedJobAction: 'IGNORE',
                            removedViewAction: 'IGNORE',
                            additionalParameters: [
                                REPO_NAME      : env.REPO_NAME,
                                ENV            : params.ENV,
                                JOB_NAME       : job.name,
                                PIPELINE_SCRIPT: pipelineScript
                            ]
                        )
                    }

                    echo "Job generation completed for ENV=${params.ENV}"
                }
            }
        }
    }
}