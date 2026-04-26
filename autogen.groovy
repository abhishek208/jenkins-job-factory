@Library('platform-lib') _
import platform.GitHub

pipeline {
    agent any

    environment {
        REPO_NAME = env.JOB_NAME.split('/')[0]
        REPO_URL  = GitHub.repoUrl(env.JOB_NAME.split('/')[0])
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main', url: "${REPO_URL}"
            }
        }

        stage('Read Workflow') {
            steps {
                script {

                    def configFile = "automation/workflow.yaml"

                    if (!fileExists(configFile)) {
                        error "workflow.yaml not found"
                    }

                    def config = readYaml file: configFile

                    def dsl = ""

                    for (job in config.jobs) {

                        def jfFile = "automation/${job.file}"

                        if (!fileExists(jfFile)) {
                            echo "Skipping ${job.name}"
                            continue
                        }

                        def scriptContent = readFile(jfFile)

                        // 🔥 DEV FOLDER JOB
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

                        // 🔥 WORKFLOW FOLDER JOB
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

                    if (dsl.trim()) {
                        jobDsl scriptText: dsl,
                            removedJobAction: 'DELETE',
                            removedViewAction: 'IGNORE'
                    }
                }
            }
        }
    }
}