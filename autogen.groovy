@Library('platform-lib') _
import platform.GitHub

pipeline {
    agent any

    parameters {
        string(name: 'REPO_NAME', description: 'Repository name')
    }

    stages {

        stage('Checkout Repo') {
            steps {
                script {

                    def repo = params.REPO_NAME
                    def url = GitHub.repoUrl(repo)

                    echo "Cloning repo: ${url}"

                    git branch: 'main', url: url
                }
            }
        }

        stage('Generate Jobs from Workflow') {
            steps {
                script {

                    def repo = params.REPO_NAME

                    def configFile = "automation/workflow.yaml"

                    if (!fileExists(configFile)) {
                        error "workflow.yaml not found in repo"
                    }

                    def config = readYaml file: configFile

                    def dsl = ""

                    for (job in config.jobs) {

                        def jfFile = "automation/${job.file}"

                        if (!fileExists(jfFile)) {
                            echo "Skipping ${job.name}, file not found"
                            continue
                        }

                        def scriptContent = readFile(jfFile)

                        // 🔥 DEV JOBS
                        dsl += """
pipelineJob("${repo}/dev/${job.name}") {
    definition {
        cps {
            script(\"\"\"${scriptContent}\"\"\")
            sandbox()
        }
    }
}
"""

                        // 🔥 WORKFLOW JOBS
                        dsl += """
pipelineJob("${repo}/workflow/${job.name}") {
    definition {
        cps {
            script(\"\"\"${scriptContent}\"\"\")
            sandbox()
        }
    }
}
"""
                    }

                    if (dsl.trim()) {
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