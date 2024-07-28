package io.github.sgpublic

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import de.undercouch.gradle.tasks.download.DownloadAction
import java.io.File

open class QQNTDownload: DefaultTask() {
    private val action: DownloadAction by lazy {
        DownloadAction(project, this)
    }
    private val qqntjsPattern = "var params= (.*?);".toRegex()

    @TaskAction
    fun download() {
        val dir = project.layout.buildDirectory.dir("linuxqq").get().asFile
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val qqntParams = NetJsonObject(
            "https://im.qq.com/rainbow/linuxQQDownload"
        ) { js ->
            qqntjsPattern.find(js)?.value?.let {
                return@let it.substring(12, it.length - 1)
            } ?: throw IllegalStateException("Cannot get qqnt version name.")
        }

        val qqntVersionName = qqntParams.get("version").asString
        extensions.add("linuxqq.verison.name", qqntVersionName)
        val qqntUrl = when (commandLine("dpkg --print-architecture")) {
            "amd64" -> qqntParams.get("x64DownloadUrl").asJsonObject.get("deb")
            "arm64" -> qqntParams.get("armDownloadUrl").asJsonObject.get("deb")
            else -> throw IllegalArgumentException("QQNT download failed: Unsupported arch!")
        }.asString
        logger.debug("QQNT download url: $qqntUrl")

        val qqntVersionCodePattern = "${qqntVersionName.replace(".", "\\.")}_\\d+_".toRegex()
        val qqntVersionCode = qqntVersionCodePattern.find(qqntUrl)?.value?.let {
            return@let it.substring(qqntVersionName.length + 1, it.length - 1).toLongOrNull()
        } ?: throw IllegalStateException("Cannot get qqnt version code.")
        extensions.add("linuxqq.verison.code", qqntVersionCode)

        val qqntVersion = "${qqntVersionName}_${qqntVersionCode}"
        extensions.add("linuxqq.version", qqntVersion)
        val qqntFile = "linuxqq-$qqntVersion.deb"
        extensions.add("linuxqq.file", qqntFile)

        logger.info("Downloading QQNT $qqntVersionName ($qqntVersionCode) ...")

        action.src(qqntUrl)
        action.dest(File(dir, qqntFile))
        action.overwrite(false)

        action.execute()
    }

    operator fun get(key: String): Any {
        return extensions.getByName(key)
    }
}
