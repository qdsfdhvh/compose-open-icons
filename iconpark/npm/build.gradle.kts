import com.github.gradle.node.task.NodeTask

plugins {
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    download = true
    version = "23.7.0"
}

val downloadIconParkIcons by tasks.register<NodeTask>("downloadIconParkIcons") {
    group = "icons"
    description = "Formats the icons for park."

    script = file("export-icons.js")
    args.set(
        listOf(
            "--output", project(":iconpark").layout.buildDirectory.dir("icons/iconpark/outline").get().asFile.absolutePath,
            "--theme", "outline",
            "--strokeWidth", "3",
        )
    )
    dependsOn(tasks.npmInstall)
}

tasks.register("generateIconParkCodes") {
    dependsOn(downloadIconParkIcons)
}
