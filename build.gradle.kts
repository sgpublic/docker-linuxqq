/*
 * This file was generated by the Gradle 'init' task.
 *
 * This is a general purpose Gradle build.
 * To learn more about Gradle by exploring our Samples at https://docs.gradle.org/8.5/samples
 */
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.bmuschko.gradle.docker.tasks.image.Dockerfile.CopyFile
import io.github.sgpublic.QQNTDownload
import io.github.sgpublic.aptInstall
import io.github.sgpublic.rm
import io.github.sgpublic.gradle.VersionGen

plugins {
    alias(libs.plugins.docker.api)
    alias(libs.plugins.release.github)
    alias(libs.plugins.buildsrc.utils)
}

group = "io.github.sgpublic"
version = "${VersionGen.COMMIT_COUNT_VERSION}"

tasks {
    val tag = "mhmzx/docker-linuxqq"

    val downloadLatestQQNT by creating(QQNTDownload::class)

    val dockerCreateDockerfile by creating(Dockerfile::class) {
        dependsOn(downloadLatestQQNT)
        doFirst {
            delete(layout.buildDirectory.file("docker-linuxqq"))
            copy {
                from("./src/main/docker/")
                include("*.sh")
                into(layout.buildDirectory.dir("docker-linuxqq"))
            }
            copy {
                from(layout.buildDirectory.dir("linuxqq"))
                include("${downloadLatestQQNT["linuxqq.file"]}")
                into(layout.buildDirectory.file("docker-linuxqq"))
            }
        }
        group = "docker"
        destFile = layout.buildDirectory.file("docker-linuxqq/Dockerfile")
        from("jlesage/baseimage-gui:${findProperty("baseimggui.version")}")
        workingDir("/tmp")
        copyFile("./startapp.sh", "/startapp.sh")
        copyFile(provider { CopyFile("./*.deb", "/tmp/") })
        val home = "/home/linuxqq"
        environmentVariable(provider {
            mapOf(
                "TZ" to "Asia/Shanghai",
                "HOME" to home,
                "APP_NAME" to "linuxqq",
                "APP_VERSION" to "${downloadLatestQQNT["linuxqq.version"]}",
                "XDG_CONFIG_HOME" to "$home/config",
            )
        })
        runCommand(provider {
            listOf(
                "mkdir -p /home/linuxqq",
                "chown 1000:1000 /home/linuxqq",
                "sed -i 's/deb.debian.org/mirrors.aliyun.com/' /etc/apt/sources.list",
                "apt-get update",
                rm(
                    "/usr/share/fonts/*",
                ),
                aptInstall(
                    "libcurl4",
                    "libnss3",
                    "libgbm-dev",
                    "libnotify-dev",
                    "libasound2",
                    "libgtk-3-0",
                    "libxss1",
                    "libxtst6",
                    "xauth",
                    "xvfb",
                    "fonts-wqy-microhei",

//                    "slirp4netns",
//                    "socat",
//                    "util-linux",
//                    "bsdmainutils",

                    "/tmp/${downloadLatestQQNT["linuxqq.file"]}",
                ),
                "apt-get clean",
                rm(
                    "/tmp/${downloadLatestQQNT["linuxqq.file"]}",
                ),
            ).joinToString(" &&\\\n ")
        })
        workingDir("/home/linuxqq")
        volume("$home/config")
    }
    val dockerBuildImage by creating(DockerBuildImage::class) {
        group = "docker"
        dependsOn(dockerCreateDockerfile)
        inputDir = layout.buildDirectory.dir("docker-linuxqq")
        dockerFile = dockerCreateDockerfile.destFile
        images.add(provider { "$tag:v${downloadLatestQQNT["linuxqq.version"]}-$version" })
        images.add(provider { "$tag:v${downloadLatestQQNT["linuxqq.version"]}" })
        images.add("$tag:latest")
        noCache = true
    }

    val dockerPushBuildBookImageOfficial by creating(DockerPushImage::class) {
        group = "docker"
        dependsOn(dockerBuildImage)
        images.add(provider { "$tag:v${downloadLatestQQNT["linuxqq.version"]}-$version" })
        images.add(provider { "$tag:v${downloadLatestQQNT["linuxqq.version"]}" })
        images.add("$tag:latest")
    }
}

fun findEnv(name: String): String {
    return findProperty(name)?.toString()?.takeIf { it.isNotBlank() }
        ?: System.getenv(name.replace(".", "_").uppercase())
}

docker {
    registryCredentials {
        username = findEnv("publishing.docker.username")
        password = findEnv("publishing.docker.password")
    }
}

githubRelease {
    token(findEnv("publishing.github.token"))
    owner = "sgpublic"
    repo = "docker-linuxqq"
    tagName = "$version"
    releaseName = "$version"
    overwrite = true
}