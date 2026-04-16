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

import kotlinx.cinterop.*
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.FileInputStream
import nl.adaptivity.xmlutil.core.impl.multiplatform.InputStreamReader
import nl.adaptivity.xmlutil.xmlStreaming
import platform.posix.S_IFMT
import platform.posix.S_IFREG
import platform.posix.stat

@OptIn(ExperimentalForeignApi::class)
class NativeResource(override val path: String) : Resource {
    override fun <R> withXmlReader(requireGeneric: Boolean, body: (XmlReader) -> R): R {
        val inStream = FileInputStream(path)
        val reader = xmlStreaming.newReader(InputStreamReader(inStream))
        return body(reader)
    }

    override fun resolve(path: String): Resource {
        return NativeResource(path.substringBeforeLast('/',"/")+path)
    }

    override fun getText(): String {
        val inStream = FileInputStream(path)
        val reader = InputStreamReader(inStream)
        val buffer = CharArray(4096)
        return buildString {
            do {
                val cnt = reader.read(buffer, 0, buffer.size)
                if (cnt > 0) appendRange(buffer, 0, cnt)
            } while (cnt >= 0)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun getResource(path: String): Resource {
    val pathBase = path.replace("//+", "/").removePrefix("/")
    var finalPath: String? = null
    memScoped {
        for (target in listOf("src/commonMain/resources", "src/commonTest/resources", "src/nativeMain/resources", "src/nativeMain/resources")) {
            val fullPath = "$target/$pathBase"
            val statResult = alloc<stat>()
            val r = stat(fullPath, statResult.ptr)
            if (r == 0 && (isRegularFile(statResult))) {
                println("Found resource $fullPath")
                finalPath = fullPath
                break
            }
        }
    }

    return NativeResource(finalPath ?: error("Could not find resource $path"))
}

@UnsafeNumber(["linux_x64: Kotlin.UInt", ])
private fun stat.getMode(): UInt {
    @Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD")
    return st_mode.toUInt()
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
private fun isRegularFile(statResult: stat): Boolean {
    return statResult.getMode() and S_IFMT.toUInt() == S_IFREG.toUInt()
}
