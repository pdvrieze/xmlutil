/*
 * Copyright (c) 2024-2026.
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

package nl.adaptivity.xmlutil.core.kxio

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import nl.adaptivity.xmlutil.*

/**
 * Create a new (platform specific) reader for the given source. This version is implemented
 * through string.
 */
public actual fun IXmlStreaming.newReader(source: Source): XmlReader {
    return newReader(source.readString())
}

/**
 * Create a new generic reader for the given source. This version is implemented
 * through string.
 */
public actual fun IXmlStreaming.newGenericReader(source: Source): XmlReader {
    return newGenericReader(source.readString())
}

/** Create a new (platform specific) writer for the given sink. */
public actual fun IXmlStreaming.newWriter(
    target: Sink,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode,
): XmlWriter {
    return newWriter(SinkAppendable(target), repairNamespaces, xmlDeclMode)
}

/** Create a new generic writer for the given sink. */
public actual fun IXmlStreaming.newGenericWriter(
    target: Sink,
    repairNamespaces: Boolean,
    xmlDeclMode: XmlDeclMode
): XmlWriter {
    return newGenericWriter(SinkAppendable(target), repairNamespaces, xmlDeclMode)
}

