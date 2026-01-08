/*
 * Copyright (c) 2025-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.xmlutil.plugin

import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URLEncoder
import java.time.LocalDateTime
import java.util.*

abstract class PublishToSonatypeTask() : DefaultTask() {

    @get:InputFile
    abstract val archive: Property<File>

    fun from(file: Provider<out File>) { archive.set(file) }
    fun from(file: File) { archive.set(file) }

    @TaskAction
    fun run() {
        val archiveFile = archive.get()
        val username = project.findProperty("ossrh.username") as String
        val password = project.findProperty("ossrh.password") as String
        check(username.isNotEmpty()) { "Missing username (ossrh.username property) " }
        check(password.isNotEmpty()) { "Missing secret (ossrh.password property) " }
        val encoded = String(Base64.getEncoder().encode("$username:$password".toByteArray()), Charsets.US_ASCII)

        val client = HttpClientBuilder.create().build()
        val versionName: String = when {
            project.isSnapshot -> {
                if ("SNAPSHOT" !in project.version.toString()) {
                    throw IllegalStateException("Attempting to publish a non-snapshot version as a snapshot: ${project.version}")
                }
                "${project.version}-${LocalDateTime.now().format(TIMESTAMP_FORMATTER)}"
            }

            else -> project.version.toString()
        }

        val deploymentName = URLEncoder.encode("XMLUtil deployment $versionName", Charsets.UTF_8)
        val deploymentType = when {
            project.isSnapshot -> "AUTOMATIC"
            else -> "USER_MANAGED"
        }
        val post = HttpPost("https://central.sonatype.com/api/v1/publisher/upload?name=$deploymentName&publishingType=$deploymentType")
        post.addHeader("Authorization", "Bearer $encoded")

        post.entity = MultipartEntityBuilder.create()
            .addBinaryBody("bundle", archiveFile)
            .build()

        val deploymentId = client.execute(post) { r ->
            r.code
            if (r.code !in 200 .. 299) {
                val responseText = r.entity.content.readAllBytes().toString(Charsets.UTF_8)
                throw IllegalStateException("Unexpected response: ${r.code} - ${r.reasonPhrase}\n  $responseText")
            }
            r.entity.content.readAllBytes().toString(Charsets.UTF_8)
        }

        logger.lifecycle("Published archive with deployment id $deploymentId")
    }


}
