/*
 * Copyright (c) 2023-2025.
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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.xmlserializable

import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.newWriter
import nl.adaptivity.xmlutil.xmlStreaming
import java.io.CharArrayWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer as MPWriter

/**
 * Extension functions for writing that need different js/jvm implementations
 */
public fun XmlSerializable.toCharArray(): CharArray {
    val caw = CharArrayWriter()
    @Suppress("CAST_NEVER_SUCCEEDS")
    xmlStreaming.newWriter(caw as MPWriter).use { writer ->
        serialize(writer)
    }
    return caw.toCharArray()
}
