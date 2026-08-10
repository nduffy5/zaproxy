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

    // Ensure japicmp runs as part of the CI pipeline to enforce the
    // network-api-baseline.xml regression gate for HttpSenderImpl.
    // The japicmp task is wired into the 'check' lifecycle in zap/zap.gradle.kts
    // (tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(japicmp) }),
    // so running ':zap:check' in CI automatically includes japicmp.
    // This block explicitly confirms that japicmp is non-skipped in CI.
    project(":zap") {
        afterEvaluate {
            tasks.matching { it.name == "japicmp" }.configureEach {
                enabled = true
                description =
                    (description ?: "") +
                        " [CI: enforced non-skipped per gradle/ci.gradle.kts]"
            }
        }
    }
}
