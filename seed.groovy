pipeline {
    agent any

    parameters {
        string(name: 'REPO_NAME', description: 'Repo name (e.g. sample-service)')
    }

    stages {

        stage('Validate') {
            steps {
                script {
                    if (!params.REPO_NAME?.trim()) {
                        error "REPO_NAME is required"
                    }

                    echo "Bootstrapping repo: ${params.REPO_NAME}"
                }
            }
        }

        stage('Create Folder + AutoGen Job') {
            steps {
                script {

                    def repo = params.REPO_NAME

                    def dsl = """
folder('${repo}')

pipelineJob('${repo}/AutoGen') {
    definition {
        cps {
            script(\"\"\"
@Library('platform-lib') _

pipeline {
    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'main')
    }

    environment {
        REPO_NAME = "${repo}"
        REPO_URL  = GitHub.repoUrl("${repo}")
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: "\${params.BRANCH}", url: "\${REPO_URL}"
            }
        }

        stage('Run AutoGen') {
            steps {
                build job: '${repo}/AutoGen-Executor', parameters: [
                    string(name: 'BRANCH', value: params.BRANCH)
                ]
            }
        }
    }
}
\"\"\")
            sandbox()
        }
    }
}
"""

                    jobDsl scriptText: dsl,
                        removedJobAction: 'IGNORE',
                        removedViewAction: 'IGNORE'
                }
            }
        }
    }
}