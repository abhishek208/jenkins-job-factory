pipeline {
    agent any

    parameters {
        string(name: 'REPO_NAME', description: 'Enter GitHub repo name')
    }

    stages {

        stage('Validate Input') {
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

                    // ---------------------------
                    // Job DSL script (STRING ONLY)
                    // ---------------------------
                    def dsl = """
folder('${repo}')

pipelineJob('${repo}/AutoGen') {
    definition {
        cps {
            script(\"\"\"
pipeline {
    agent any

    parameters {
        string(name: 'REPO_NAME', defaultValue: '${repo}')
    }

    stages {
        stage('Run AutoGen Engine') {
            steps {
                build job: '${repo}/AutoGen', parameters: [
                    string(name: 'REPO_NAME', value: '${repo}')
                ]
            }
        }
    }
}
\"\"\")
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

        stage('Trigger AutoGen (first run)') {
            steps {
                script {
                    def repo = params.REPO_NAME

                    build job: "${repo}/AutoGen", parameters: [
                        string(name: 'REPO_NAME', value: repo)
                    ]
                }
            }
        }
    }
}