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
import nl.adaptivity.xmlutil.core.impl.multiplatform.InputStream
import nl.adaptivity.xmlutil.newGenericReader
import nl.adaptivity.xmlutil.newReader
import nl.adaptivity.xmlutil.xmlStreaming
import java.net.URL

class JvmResource(val url: URL) : Resource {
    override val path: String get() = url.path

    override fun <R> withXmlReader(requireGeneric: Boolean, body: (XmlReader) -> R): R {
        return url.openStream().use { inStream ->
            val newReader = when {
                requireGeneric -> xmlStreaming.newGenericReader(inStream, "UTF-8")
                else -> xmlStreaming.newReader(inStream, "UTF-8")
            }

            newReader.use(body)
        }
    }

    @Suppress("DEPRECATION")
    override fun resolve(path: String): Resource {
        return JvmResource(URL(url, path))
    }

    override fun getText(): String {
        return url.readText()
    }
}

actual fun getResource(path: String): Resource {
    val url = Resource::class.java.getResource(path) ?: throw IllegalArgumentException("Resource not found: $path")
    return JvmResource(url)
}

fun Resource.openStream(): InputStream {
    return (this as JvmResource).url.openStream()
}
