// ===============================
// autogen.groovy
// ===============================

@Library('platform-lib') _

import platform.GitHub
import platform.JobFactory

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
        REPO_NAME        = "${env.JOB_NAME.split('/')[0]}"
        REPO_URL         = "${GitHub.repoUrl(env.JOB_NAME.split('/')[0])}"
        JOB_FACTORY_URL  = "${JobFactory.repoUrl()}"
    }

    stages {

        stage('Init') {
            steps {
                script {
                    echo "Repo Name        : ${env.REPO_NAME}"
                    echo "Repo URL         : ${env.REPO_URL}"
                    echo "Job Factory URL  : ${env.JOB_FACTORY_URL}"
                    echo "Branch           : ${params.BRANCH}"
                    echo "ENV              : ${params.ENV}"
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

        stage('Checkout Job Factory') {
            steps {
                dir('job-factory') {
                    git(
                        branch: 'main',
                        url: "${env.JOB_FACTORY_URL}"
                    )
                }
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
                            targets: 'job-factory/jobs/generatedJobs.groovy',
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
