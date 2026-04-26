@Library('platform-lib') _

import platform.GitHub

pipeline {
    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Branch to build')
    }

    environment {
        // repo name derived from Seed-created structure
        REPO_NAME = "${env.JOB_NAME.split('/')[0]}"
        REPO_URL  = GitHub.repoUrl("${env.JOB_NAME.split('/')[0]}")
    }

    stages {

        stage('Init') {
            steps {
                script {
                    echo "Repo Name: ${REPO_NAME}"
                    echo "Repo URL: ${REPO_URL}"
                    echo "Branch: ${params.BRANCH}"
                }
            }
        }

        stage('Checkout Repo') {
            steps {
                git branch: "${params.BRANCH}", url: "${REPO_URL}"
            }
        }

        stage('Validate Workflow') {
            steps {
                script {
                    def wf = "automation/workflow.yaml"

                    if (!fileExists(wf)) {
                        error "workflow.yaml not found"
                    }

                    echo "workflow.yaml found"
                }
            }
        }

        stage('Generate Jobs') {
            steps {
                script {

                    def config = readYaml file: 'automation/workflow.yaml'

                    def dsl = ""

                    for (job in config.jobs) {

                        def jfFile = "automation/${job.file}"

                        if (!fileExists(jfFile)) {
                            echo "Skipping ${job.name}, missing ${jfFile}"
                            continue
                        }

                        def pipelineScript = readFile(jfFile)

                        // 🔵 DEV JOBS
                        dsl += """
pipelineJob("${REPO_NAME}/dev/${job.name}") {
    definition {
        cps {
            script(\"\"\"
${pipelineScript}
\"\"\")
            sandbox()
        }
    }
}
"""

                        // 🔵 WORKFLOW JOBS
                        dsl += """
pipelineJob("${REPO_NAME}/workflow/${job.name}") {
    definition {
        cps {
            script(\"\"\"
${pipelineScript}
\"\"\")
            sandbox()
        }
    }
}
"""
                    }

                    if (dsl?.trim()) {
                        jobDsl scriptText: dsl,
                            removedJobAction: 'DELETE',
                            removedViewAction: 'IGNORE'
                    } else {
                        echo "No jobs generated from workflow.yaml"
                    }
                }
            }
        }
    }
}