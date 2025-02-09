import de.undercouch.gradle.tasks.download.Download

plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.compose") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("de.undercouch.download") version "5.6.0"
}

group = "io.github.qdsfdhvh.iconpark"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/valkyrie/commonMain/kotlin")
            dependencies {
                implementation(compose.ui)
                implementation(compose.materialIconsExtended)
            }
        }
    }
    jvmToolchain(11)
}

val iconParkVersion = "1.4.2"
val valkyrieVersion = "0.11.1"

val downloadIconParkIcons by tasks.register<Download>("downloadIconParkIcons") {
    group = "icons"
    description = "Downloads the icons for park."
    src("https://github.com/bytedance/IconPark/archive/refs/tags/v${iconParkVersion}.zip")
    dest(layout.buildDirectory)
    overwrite(false)
}

val unzipIconParkIcons by tasks.register<Copy>("unzipIconParkIcons") {
    dependsOn(downloadIconParkIcons)
    group = "icons"
    description = "Unpacks the icons for park."
    from(zipTree(layout.buildDirectory.file("IconPark-${iconParkVersion}.zip")))
    into(layout.buildDirectory)
}

val downloadValkyrieCli by tasks.register<Download>("downloadValkyrieCli") {
    group = "icons"
    description = "Downloads the Valkyrie cli."
    src("https://github.com/ComposeGears/Valkyrie/releases/download/0.${valkyrieVersion}/valkyrie-cli-${valkyrieVersion}.zip")
    dest(layout.buildDirectory)
    overwrite(false)
}

val unzipValkyrieCli by tasks.register<Copy>("unzipValkyrieCli") {
    dependsOn(downloadValkyrieCli)
    group = "icons"
    description = "Unpacks the Valkyrie cli."
    from(zipTree(layout.buildDirectory.file("valkyrie-cli-${valkyrieVersion}.zip")))
    into(layout.buildDirectory)
}


val generateIconParkCodes by tasks.register<GenerateIconParkCodes>("generateIconParkCodes") {
    dependsOn(unzipIconParkIcons)
    dependsOn(unzipValkyrieCli)
    group = "icons"
    description = "Generates the icons for park."

    shellPath = layout.buildDirectory.file("bin/valkyrie")
    sourceDir = layout.buildDirectory.dir("IconPark-${iconParkVersion}/source")
    outputDir = layout.buildDirectory.dir("generated/valkyrie/commonMain/kotlin")
    packageName = "io.github.qdsfdhvh.iconpark"
}

abstract class GenerateIconParkCodes : DefaultTask() {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:InputFile
    abstract val shellPath: RegularFileProperty

    @get:Input
    abstract val packageName: Property<String>

    @TaskAction
    fun run() {
        val childrenFileLists = sourceDir.get().asFile.listFiles()

        val codesDir = outputDir.dir(packageName.get().replace(".", "/"))

        val childObjectNames = ArrayList<String>()
        childrenFileLists.forEach { file ->
            if (file.isDirectory) {
                childObjectNames.add(file.name)
                workerExecutor.noIsolation().submit(GenerateIconWorker::class.java) {
                    inputPath.set(file)
                    outputPath.set(codesDir)
                    shellPath.set(this@GenerateIconParkCodes.shellPath)
                    finalPackageName.set(this@GenerateIconParkCodes.packageName)
                }
            }
        }

        val generatedCode = """
            |package ${packageName.get()}
            |
            |object IconPark {
            |${childObjectNames.joinToString("\n") { "    object $it" }}
            |}
        """.trimMargin()
        val file = codesDir.get().file("IconPark.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(generatedCode)

        workerExecutor.await()
    }
}

abstract class GenerateIconWorker : WorkAction<GenerateIconWorker.Parameters> {
    interface Parameters : WorkParameters {
        val shellPath: RegularFileProperty
        val inputPath: Property<File>
        val outputPath: DirectoryProperty
        val finalPackageName: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun execute() {
        val shellPath = parameters.shellPath.orNull
        val inputPath = parameters.inputPath.get()
        val outputPath = parameters.outputPath.get()
        val packageName = parameters.finalPackageName.get()
        execOperations.exec {
            commandLine(
                shellPath,
                "svgxml2imagevector",
                "--input-path=${inputPath.absolutePath}",
                "--output-path=${outputPath.asFile.absolutePath}",
                "--package-name=${packageName}",
                "--iconpack-name=IconPark",
                "--nested-pack-name=${inputPath.name}",
            )
        }
    }
}

