folder("${REPO_NAME}")

pipelineJob("${REPO_NAME}/AutoGen") {
    description("AutoGen job for ${REPO_NAME}")

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url("${REPO_URL}")
                    }
                    branches("main")
                }
            }
            scriptPath("autogen.groovy")
        }
    }
}