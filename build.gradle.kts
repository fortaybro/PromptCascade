// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

/*
 * Build output goes to "build.nosync" instead of "build" so iCloud
 * stops copying build files mid-build.
 */
allprojects {
    layout.buildDirectory.set(
        layout.projectDirectory.dir("build.nosync")
    )
}