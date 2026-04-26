@Library('platform-lib') _

import platform.GitHub

pipeline {
    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'main')
        choice(name: 'TARGET_ENV', choices: ['dev', 'workflow', 'all'], description: 'Which env to generate')
    }

    environment {
        REPO_NAME = "${env.JOB_NAME.split('/')[0]}"
        REPO_URL  = GitHub.repoUrl("${env.JOB_NAME.split('/')[0]}")
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: "${params.BRANCH}", url: "${REPO_URL}"
            }
        }

        stage('Validate Workflow') {
            steps {
                script {
                    if (!fileExists("automation/workflow.yaml")) {
                        error "workflow.yaml not found"
                    }
                }
            }
        }

        stage('Generate Jobs') {
            steps {
                script {

                    def config = readYaml file: 'automation/workflow.yaml'

                    def dsl = ""

                    config.jobs.each { job ->

                        def scriptFile = "automation/${job.file}"

                        if (!fileExists(scriptFile)) {
                            echo "Skipping ${job.name}"
                            return
                        }

                        def scriptContent = readFile(scriptFile)

                        // -------------------------
                        // DEV JOBS
                        // -------------------------
                        if (params.TARGET_ENV == 'dev' || params.TARGET_ENV == 'all') {

                            dsl += """
pipelineJob("${REPO_NAME}/dev/${job.name}") {
    definition {
        cps {
            script(\"\"\"
${scriptContent}
\"\"\")
            sandbox()
        }
    }
}
"""
                        }

                        // -------------------------
                        // WORKFLOW JOBS
                        // -------------------------
                        if (params.TARGET_ENV == 'workflow' || params.TARGET_ENV == 'all') {

                            dsl += """
pipelineJob("${REPO_NAME}/workflow/${job.name}") {
    definition {
        cps {
            script(\"\"\"
${scriptContent}
\"\"\")
            sandbox()
        }
    }
}
"""
                        }
                    }

                    if (dsl?.trim()) {
                        jobDsl scriptText: dsl,
                            removedJobAction: 'IGNORE',
                            removedViewAction: 'IGNORE'
                    } else {
                        echo "No jobs generated"
                    }
                }
            }
        }
    }
}