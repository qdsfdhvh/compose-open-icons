import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform") version "2.0.0" apply false
    id("com.vanniktech.maven.publish") version "0.30.0"
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
    signAllPublications()
}
