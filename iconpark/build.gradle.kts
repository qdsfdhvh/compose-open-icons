import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose") version "2.0.0"
    id("com.android.library")
    id("org.jetbrains.compose") version "1.7.3"
    id("de.undercouch.download") version "5.6.0"
    id("com.vanniktech.maven.publish")
}

kotlin {
    androidTarget()
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    js {
        browser()
    }
    @Suppress("OPT_IN_USAGE")
    wasmJs {
        browser()
    }
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/valkyrie/commonMain/kotlin")
            dependencies {
                implementation(compose.ui)
            }
        }
    }
    jvmToolchain(11)
}

android {
    namespace = "io.github.qdsfdhvh.iconpark"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}

val valkyrieVersion = "0.11.1"

val downloadValkyrieCli by tasks.register<Download>("downloadValkyrieCli") {
    group = "icons"
    description = "Downloads the Valkyrie cli."
    src("https://github.com/ComposeGears/Valkyrie/releases/download/${valkyrieVersion}/valkyrie-cli-${valkyrieVersion}.zip")
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
    dependsOn(unzipValkyrieCli)
    group = "icons"
    description = "Generates the icons for park."

    shellPath = layout.buildDirectory.file("bin/valkyrie")
    inputDir = layout.buildDirectory.dir("icons/iconpark/outline")
    outputDir = layout.buildDirectory.dir("generated/valkyrie/commonMain/kotlin")
    packageName = "io.github.qdsfdhvh.iconpark"
    iconPackName = "IconParkIcons"
    nestedPackName = "Outline"
}

tasks["compileKotlinMetadata"].dependsOn(generateIconParkCodes)

abstract class GenerateIconParkCodes : DefaultTask() {

    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:InputFile
    abstract val shellPath: RegularFileProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val iconPackName: Property<String>

    @get:Input
    abstract val nestedPackName: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun run() {
        val codesDir = outputDir.dir(packageName.get().replace(".", "/"))

        execOperations.exec {
            commandLine(
                shellPath.get().asFile.absolutePath,
                "svgxml2imagevector",
                "--input-path=${inputDir.get().asFile.absolutePath}",
                "--output-path=${codesDir.get().asFile.absolutePath}",
                "--package-name=${packageName.get()}",
                "--iconpack-name=${iconPackName.get()}",
                "--nested-pack-name=${nestedPackName.get()}",
            )
        }
    }
}

val generateIconParkSwiftCodes by tasks.register<GenerateIconParkSwiftCodes>("generateIconParkSwiftCodes") {
    inputDir = layout.buildDirectory.dir("icons/iconpark/outline")
    outputDir = project.file("Sources/iconpark/Resources/Media.xcassets/")
    warningLogDir = projectDir
}

abstract class GenerateIconParkSwiftCodes : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val warningLogDir: DirectoryProperty

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun run() {
        val workQueue = workerExecutor.noIsolation()

        if (!outputDir.get().asFile.exists()) {
            outputDir.get().asFile.mkdirs()
        }

        val waringCollectDir = warningLogDir.dir(".warnings").get().asFile
        if (waringCollectDir.exists()) {
            waringCollectDir.deleteRecursively()
        } else {
            waringCollectDir.mkdirs()
        }

        val childrenFileLists = inputDir.get().asFile.listFiles()
        childrenFileLists?.forEach { file ->
            if (file.isFile && file.name.endsWith(".svg")) {
                workQueue.submit(GenerateSwiftIconWorker::class.java) {
                    inputPath.set(file)
                    outputPath.set(outputDir)
                    waringDir.set(waringCollectDir)
                }
            }
        }

        workQueue.await()

        val warningFile = warningLogDir.get().file("Warning.txt").asFile
        if (warningFile.exists()) {
            warningFile.delete()
        }
        warningFile.createNewFile()

        warningFile.bufferedWriter().use {
            waringCollectDir.listFiles()?.forEach { file ->
                it.appendLine(file.readText())
            }
            waringCollectDir.deleteRecursively()
        }
    }
}

abstract class GenerateSwiftIconWorker : WorkAction<GenerateSwiftIconWorker.Parameters> {
    interface Parameters : WorkParameters {
        val inputPath: Property<File>
        val outputPath: DirectoryProperty
        val waringDir: DirectoryProperty
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun execute() {
        val inputPath = parameters.inputPath.get()

        val svgName = inputPath.name.removeSuffix(".svg")

        // swiftdraw not support create parent dir
        val outputDir = parameters.outputPath.dir("${svgName}-symbol.symbolset").get().asFile
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        println("Convert ${inputPath.absolutePath} to $outputDir")

        val result = execOperations.exec {
            commandLine(
                "swiftdraw", inputPath.absolutePath,
                "--format", "sfsymbol",
                "--output", outputDir.resolve("${svgName}-symbol.svg").absolutePath,
            )
            isIgnoreExitValue = true
        }

        if (result.exitValue != 0) {
            val logFile = parameters.waringDir.get().file(svgName).asFile
            logFile.createNewFile()
            logFile.bufferedWriter().use {
                it.write("Can't Convert $svgName")
            }
        }
    }
}
