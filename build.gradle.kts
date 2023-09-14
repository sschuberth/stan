import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.gver)
    alias(libs.plugins.versionCatalogUpdate)
    alias(libs.plugins.versions)
}

versionCatalogUpdate {
    // Keep the custom sorting / grouping.
    sortByKey.set(false)
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    gradleReleaseChannel = "current"
    outputFormatter = "json"

    val nonFinalQualifiers = listOf(
        "alpha", "b", "beta", "cr", "dev", "ea", "eap", "m", "milestone", "pr", "preview", "rc", "\\d{14}"
    ).joinToString("|", "(", ")")

    val nonFinalQualifiersRegex = Regex(".*[.-]$nonFinalQualifiers[.\\d-+]*", RegexOption.IGNORE_CASE)

    rejectVersionIf {
        candidate.version.matches(nonFinalQualifiersRegex)
    }
}
