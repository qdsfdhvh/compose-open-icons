import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform") version "2.0.0" apply false
    id("com.android.library") version "8.3.0" apply false
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        @Suppress("UnstableApiUsage")
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
            signAllPublications()
            pom {
                name.set(project.name)
                description.set("some open icons convert to compose.")
            }
        }
    }
}