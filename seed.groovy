@Library('platform-lib') _

def repoName = REPO_NAME

folder(repoName)

pipelineJob("${repoName}/AutoGen") {
    description("Auto generator for ${repoName}")

    definition {
        cps {
            script("""
pipeline {
    agent any

    stages {

        stage('Run AutoGen') {
            steps {
                script {
                    build job: '${repoName}/AutoGen-Engine'
                }
            }
        }

    }
}
""")
            sandbox()
        }
    }
}