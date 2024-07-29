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
import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import io.github.sgpublic.QQNTInfo
import io.github.sgpublic.command
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

    val qqntInfo by creating(QQNTInfo::class) {
        token = findEnv("publishing.gitlab.token")
    }

    val dockerCreateDockerfile by creating(Dockerfile::class) {
        doFirst {
            delete(layout.buildDirectory.file("docker-linuxqq"))
            copy {
                from("./src/main/docker/")
                include("*.sh")
                into(layout.buildDirectory.dir("docker-linuxqq"))
            }
        }
        group = "docker"
        destFile = layout.buildDirectory.file("docker-linuxqq/Dockerfile")

        // 安装 QQNT
        from(Dockerfile.From("jlesage/baseimage-gui:${findProperty("baseimggui.version")}").withStage("qqntinstaller"))
        runCommand(provider {
            command(
                "apt-get update",
                aptInstall(
                    "curl",
                ),
                "mkdir -p /tmp",
                "curl -o /tmp/${qqntInfo["linuxqq.file"]} --retry 5 --retry-delay 3 ${qqntInfo["linuxqq.url"]}",
                aptInstall(
                    "/tmp/${qqntInfo["linuxqq.file"]}",
                ),
                rm(
                    "/tmp/${qqntInfo["linuxqq.file"]}",
                    "/usr/share/fonts/*",
                ),
            )
        })

        // 安装依赖
        from(Dockerfile.From("jlesage/baseimage-gui:${findProperty("baseimggui.version")}").withStage("deps"))
        runCommand(command(
            "apt-get update",
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

//                    "slirp4netns",
//                    "socat",
//                    "util-linux",
//                    "bsdmainutils",
            ),
            rm(
                "/usr/share/fonts/*",
            ),
            aptInstall(
                "fonts-wqy-microhei",
            )
        ))

        // 最终构建
        from(Dockerfile.From("jlesage/baseimage-gui:${findProperty("baseimggui.version")}"))
        runCommand(command(
            "mkdir -p /home/linuxqq",
            "chown 1000:1000 /home/linuxqq",
        ))
        workingDir("/home/linuxqq")
        copyFile(CopyFile("/", "/").withStage("deps"))
        copyFile(CopyFile("/", "/").withStage("qqntinstaller"))
        copyFile("./startapp.sh", "/startapp.sh")
        val home = "/home/linuxqq"
        environmentVariable(provider {
            mapOf(
                "TZ" to "Asia/Shanghai",
                "HOME" to home,
                "APP_NAME" to "linuxqq",
                "APP_VERSION" to "${qqntInfo["linuxqq.version"]}",
                "XDG_CONFIG_HOME" to "$home/config",
                "QQ_HOME" to "/opt/QQ",
            )
        })
        volume("$home/config")
    }
    val dockerBuildImage by creating(DockerBuildImage::class) {
        group = "docker"
        dependsOn(dockerCreateDockerfile)
        inputDir = layout.buildDirectory.dir("docker-linuxqq")
        dockerFile = dockerCreateDockerfile.destFile
        images.add(provider { "$tag:v${qqntInfo["linuxqq.version"]}-${qqntInfo["dockerimage.version"]}" })
        images.add(provider { "$tag:v${qqntInfo["linuxqq.version"]}" })
        images.add("$tag:latest")
        noCache = true
    }

    val dockerPushBuildBookImageOfficial by creating(DockerPushImage::class) {
        group = "docker"
        dependsOn(dockerBuildImage)
        images.add(provider { "$tag:v${qqntInfo["linuxqq.version"]}-${qqntInfo["dockerimage.version"]}" })
        images.add(provider { "$tag:v${qqntInfo["linuxqq.version"]}" })
        images.add("$tag:latest")
    }

    val githubRelease by getting(GithubReleaseTask::class) {
        authorization = provider {
            "Token ${findEnv("publishing.github.token").get()}"
        }
        owner = "sgpublic"
        repo = "docker-linuxqq"
        tagName = provider { "v${qqntInfo["linuxqq.version"]}-${qqntInfo["dockerimage.version"]}" }
        releaseName = provider { "v${qqntInfo["linuxqq.version"]}-${qqntInfo["dockerimage.version"]}" }
        overwrite = true
    }
}

fun findEnv(name: String) = provider {
    findProperty(name)?.toString()?.takeIf { it.isNotBlank() }
        ?: System.getenv(name.replace(".", "_").uppercase())
}

docker {
    registryCredentials {
        username = findEnv("publishing.docker.username")
        password = findEnv("publishing.docker.password")
    }
}
