import org.gradle.api.GradleException
import java.io.IOException

rootProject.name = "Chat-OG"

val skipBuildPythonCheck = System.getenv("PRE_BUILD_PYTHON_RUN")?.toBoolean() ?: false

if (!skipBuildPythonCheck) {
    val isBuildPythonOnly = (
        gradle.startParameter.taskNames.size == 1 &&
        gradle.startParameter.taskNames[0].equals("buildPython", ignoreCase = true)
    )
    val customMavenLocal = System.getProperty("SELF_MAVEN_LOCAL_REPO")
    if (!customMavenLocal.isNullOrEmpty() && isBuildPythonOnly) {
        println("TrueOG Bootstrap detected. Running './gradlew buildPython' as a pre-build step...")
        try {
            val processBuilder = ProcessBuilder("./gradlew", "buildPython").directory(rootDir).inheritIO()
            val env = processBuilder.environment()
            env["PRE_BUILD_PYTHON_RUN"] = "true"
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw GradleException("ERROR: Pre-build step './gradlew buildPython' failed with exit code $exitCode")
            } else {
                println("Building Chat-OG translation support completed successfully.")
            }
        } catch (e: IOException) {
            throw GradleException("Failed to build Chat-OG with translation support: ${e.message}", e)
        } catch (e: InterruptedException) {
            throw GradleException("Building Chat-OG with translation support was interrupted: ${e.message}", e)
        }
    }
    else if (!isBuildPythonOnly) {
        println("WARNING: Chat-OG will not be built with translation support. Either run './gradlew buildPython' alone before './gradlew build', or use the TrueOG Bootstrap.")
    }
}

