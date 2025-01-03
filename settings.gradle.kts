rootProject.name = "Chat-OG"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

val customMavenLocal = System.getProperty("SELF_MAVEN_LOCAL_REPO")

if (!customMavenLocal.isNullOrEmpty()) {
    logger.lifecycle("TrueOG Bootstrap detected. Adding buildPython to start of task list for translation functionality.")

    gradle.settingsEvaluated {
        val originalTasks = gradle.startParameter.taskNames
        if (!originalTasks.contains("buildPython")) {
            gradle.startParameter.setTaskNames(listOf("buildPython") + originalTasks)
        }
    }
} else {
    logger.lifecycle("WARNING: buildPython task not specified. Translator will not function without it.")
}
