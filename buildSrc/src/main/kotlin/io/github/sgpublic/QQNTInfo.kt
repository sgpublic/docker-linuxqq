package io.github.sgpublic

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class QQNTInfo: DefaultTask() {
    override fun getGroup() = "linuxqq"

    private val qqntjsPattern = "var params= (.*?);".toRegex()
    private var cache: JsonObject? = null

    @TaskAction
    fun execute(): JsonObject {
        val content = JsonObject()
        val qqntParams = NetJsonObject(
            "https://im.qq.com/rainbow/linuxQQDownload"
        ) { js ->
            qqntjsPattern.find(js)?.value?.let {
                return@let it.substring(12, it.length - 1)
            } ?: throw IllegalStateException("Cannot get qqnt version name.")
        }

        val qqntVersionName = qqntParams.get("version").asString
        content.add("linuxqq.version.name", JsonPrimitive(qqntVersionName))
        val qqntUrl = when (commandLine("dpkg --print-architecture")) {
            "amd64" -> qqntParams.get("x64DownloadUrl").asJsonObject.get("deb")
            "arm64" -> qqntParams.get("armDownloadUrl").asJsonObject.get("deb")
            else -> throw IllegalArgumentException("QQNT download failed: Unsupported arch!")
        }.asString
        logger.debug("QQNT download url: $qqntUrl")
        content.add("linuxqq.url", JsonPrimitive(qqntUrl))

        val qqntVersionCodePattern = "${qqntVersionName.replace(".", "\\.")}_\\d+_".toRegex()
        val qqntVersionCode = qqntVersionCodePattern.find(qqntUrl)?.value?.let {
            return@let it.substring(qqntVersionName.length + 1, it.length - 1).toLongOrNull()
        } ?: throw IllegalStateException("Cannot get qqnt version code.")
        content.add("linuxqq.version.code", JsonPrimitive(qqntVersionCode))

        val qqntVersion = "${qqntVersionName}_${qqntVersionCode}"
        content.add("linuxqq.version", JsonPrimitive(qqntVersion))
        val qqntFile = "linuxqq-$qqntVersion.deb"
        content.add("linuxqq.file", JsonPrimitive(qqntFile))

        var dockerImageVersion = project.version.toString().toLong()
        content.add("dockerimage.version", JsonPrimitive(dockerImageVersion))

        if (content != readCache()) {
            dockerImageVersion += 1
            content.add("dockerimage.version", JsonPrimitive(dockerImageVersion))
            cacheFile.writeText(GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(content))
            this.cache = content
        }

        return content
    }

    private fun readCache(): JsonObject? {
        return cacheFile.takeIf { it.exists() }?.let {
            return@let Gson().fromJson(it.reader(), JsonObject::class.java)
        }
    }

    operator fun get(key: String): Any {
        val cache = this.cache ?: readCache() ?: execute()
        (cache.get(key) as JsonPrimitive).let {
            return when {
                it.isNumber -> it.asLong
                it.isBoolean -> it.isBoolean
                else -> it.asString
            }
        }
    }
}

val Task.cacheFile: File get() = File(project.rootDir, "linuxqq.json")
