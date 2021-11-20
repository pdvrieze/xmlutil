/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.test

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import java.io.InputStreamReader

abstract class ResourceTestBase(val baseDir: String) {
    fun deserializeXsd(fileName: String): XSSchema {
        val resourceName = "${baseDir.dropLastWhile { it=='/' }}/$fileName"
        return javaClass.classLoader.getResourceAsStream(resourceName)!!.use { inStream ->
            XmlStreaming.newGenericReader(inStream, "UTF-8").use { reader ->
                format.decodeFromReader(XSSchema.serializer(), reader)
            }
        }
    }

    fun readResourceAsString(fileName: String): String {
        val resourceName = "${baseDir.dropLastWhile { it=='/' }}/$fileName"
        return javaClass.classLoader.getResourceAsStream(resourceName)!!.use { inStream ->
            InputStreamReader(inStream).use { reader ->
                reader.readText()
            }
        }
    }

    companion object {
        val format = XML {
            autoPolymorphic = true
            indent = 4
//            isCollectingNSAttributes = true
        }
    }
}
