pipelineJob("${REPO_NAME}/${ENV}/${JOB_NAME}") {
    description("Generated job for ${JOB_NAME}")

    definition {
        cps {
            script(PIPELINE_SCRIPT)
            sandbox()
        }
    }
}