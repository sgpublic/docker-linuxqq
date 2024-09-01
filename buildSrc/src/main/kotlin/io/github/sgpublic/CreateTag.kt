package io.github.sgpublic

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class CreateTag: DefaultTask() {
    override fun getGroup(): String {
        return "publishing"
    }

    @get:Input
    abstract val token: Property<String>

    @TaskAction
    fun execute() {
        Git.open(project.rootDir).use { git ->
            val content = cacheFile.reader().use {
                Gson().fromJson(it, JsonObject::class.java)
            }
            val qqntVersion = content.get("linuxqq.version").asString
            val dockerImageVersion = content.get("dockerimage.version").asInt
            git.add().addFilepattern("linuxqq.json").call()
            git.commit()
                .setMessage("chore(linuxqq): update linuxqq $qqntVersion")
                .setAuthor("updater", "updater@example.com")
                .call()
            git.tag()
                .setName("v${qqntVersion}-${dockerImageVersion}")
                .call()
            git.push()
                .also {
                    token.orNull?.let { token ->
                        it.setCredentialsProvider(UsernamePasswordCredentialsProvider("mhmzx", token))
                    }
                }
                .setPushAll().setPushTags().call()
        }
    }
}