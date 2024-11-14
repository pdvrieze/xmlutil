/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil.core.kxio

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import nl.adaptivity.xmlutil.*
import java.io.OutputStreamWriter

public actual fun IXmlStreaming.newReader(source: Source): XmlReader {
    return newReader(source.asInputStream())
}

public actual fun IXmlStreaming.newGenericReader(source: Source): XmlReader {
    return newGenericReader(source.asInputStream())
}

public actual fun IXmlStreaming.newWriter(
    target: Sink,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode,
): XmlWriter {
    val output = OutputStreamWriter(target.asOutputStream(), Charsets.UTF_8)
    return newWriter(output, repairNamespaces, xmlDeclMode).onClose { output.close() }
}

public actual fun IXmlStreaming.newGenericWriter(
    target: Sink,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode,
): XmlWriter {
    val output = OutputStreamWriter(target.asOutputStream(), Charsets.UTF_8)
    return newGenericWriter(output, repairNamespaces, xmlDeclMode).onClose { output.close() }
}
