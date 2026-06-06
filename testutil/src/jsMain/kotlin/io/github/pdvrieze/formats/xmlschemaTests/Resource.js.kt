/*
 * Copyright (c) 2026.
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

package io.github.pdvrieze.formats.xmlschemaTests

import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.xmlStreaming

private val isNode: Boolean = js("typeof process !== 'undefined' && process.versions != null && process.versions.node != null") as Boolean

class JsResource(override val path: String) : Resource {
    override fun <R> withXmlReader(requireGeneric: Boolean, body: (XmlReader) -> R): R {
        val content = getText()
        val newReader = when {
            requireGeneric -> xmlStreaming.newGenericReader(content)
            else -> xmlStreaming.newReader(content)
        }
        return newReader.use(body)
    }

    override fun resolve(path: String): Resource {
        val newPath = if (isNode) {
            val pathModule = js("eval('require')('path')")
            pathModule.resolve(pathModule.dirname(this.path), path) as String
        } else {
            val url = js("new URL(path, 'http://localhost/' + this.path)")

            this.path.substringAfterLast('/', "/")+path
                .replace("//+","/")
        }
        return JsResource(newPath)
    }

    override fun getText(): String {
        return if (isNode) {
            val fs = js("eval('require')('fs')")
            fs.readFileSync(path, "utf8") as String
        } else run {
            val xhr = js("new XMLHttpRequest()")
            xhr.open("GET", path, false)
            xhr.send()
            xhr.responseText as String
        }
    }
}

actual fun getResource(path: String): Resource {
    if (isNode) {
        val pathModule = js("eval('require')('path')")
        val fs = js("eval('require')('fs')")

        var fullPath = pathModule.resolve(path.removePrefix("/")) as String
        if (!fs.existsSync(fullPath)) {
            // Try relative to some known base if needed
            fullPath = pathModule.resolve("kotlin", path.removePrefix("/")) as String
        }
        return JsResource(fullPath)
    } else {
        val basePath = "/base/kotlin/"
        val correctedPath = if (path.startsWith("/")) path.removePrefix("/") else path
        return JsResource("$basePath/$correctedPath".replace(Regex("//+"), "/"))
    }
}
