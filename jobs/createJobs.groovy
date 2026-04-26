folder("${REPO_NAME}")

pipelineJob("${REPO_NAME}/AutoGen") {
    description("AutoGen job for ${REPO_NAME}")

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url("https://github.com/abhishek208/jenkins-job-factory.git")
                    }
                    branches("main")
                }
            }
            scriptPath("autogen.groovy")
        }
    }
}