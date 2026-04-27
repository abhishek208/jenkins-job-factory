folder("${REPO_NAME}") {
    description("Jobs for ${REPO_NAME}")
}

folder("${REPO_NAME}/${ENV}") {
    description("${ENV} jobs for ${REPO_NAME}")
}

pipelineJob("${REPO_NAME}/${ENV}/${JOB_NAME}") {
    description("Generated job for ${JOB_NAME}")

    definition {
        cps {
            script(PIPELINE_SCRIPT)
            sandbox()
        }
    }
}
