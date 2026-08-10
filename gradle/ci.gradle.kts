// Build tweaks when running in GitHub CI

fun isEnvVarTrue(envvar: String) = System.getenv(envvar) == "true"

if (isEnvVarTrue("CI") && System.getenv("GITHUB_WORKFLOW") == "Java CI") {

    allprojects {
        tasks.withType(Test::class).configureEach {
            testLogging {
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }

    // Ensure japicmp runs as a non-skipped step in CI to enforce the network API baseline.
    // The baseline is recorded in docs/network-api-baseline.xml and covers the public API
    // surface of org.zaproxy.zap.network (including HttpSenderImpl and related types).
    // Any binary-incompatible change to those types will cause this step to fail.
    project(":zap") {
        tasks.named("japicmp") {
            enabled = true
        }
    }

}
