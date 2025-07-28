/*
 * Copyright (c) 2025.
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

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.joda.time.Instant
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
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
        val authHeader = "UserToken ${Base64.getEncoder().encode("$username:$password".toByteArray())}"

        val url = URI("https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED")
        val connection = url.toURL().openConnection() as HttpURLConnection
        try {
            val boundary = "*******${System.currentTimeMillis()}*******"

            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", authHeader)
            connection.setRequestProperty("Accept", "text/plain")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            connection.getOutputStream().use { output ->
                output.write("--$boundary\r\n".toByteArray())
                output.write("Content-Disposition: form-data; name=\"bundle\"; filename=\"${archiveFile.name}\"\r\n".toByteArray())
                output.write("Content-Type: application/octet-stream\r\n".toByteArray())

                archiveFile.inputStream().copyTo(output)
                output.write("\r\n--$boundary\r\n".toByteArray())
            }

            connection.connect()

            if (connection.responseCode !in 200 .. 299) {
                throw IllegalStateException("Unexpected response: ${connection.responseCode} - ${connection.responseMessage} ")
            }

            val deploymentId =  connection.getInputStream().readAllBytes().toString(Charsets.UTF_8)

            logger.lifecycle("Published archive with deployment id $deploymentId")
        } finally {
            connection.disconnect()
        }
    }


}

fun Project.f() {
    val f: File = file("Foo")
}
