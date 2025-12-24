/*
 * Copyright (c) 2024-2025.
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
import nl.adaptivity.xmlutil.IXmlStreaming
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter

/** Create a new (platform specific) reader for the given source. */
expect public fun IXmlStreaming.newReader(source: Source): XmlReader

/** Create a new generic reader for the given source. */
expect public fun IXmlStreaming.newGenericReader(source: Source): XmlReader

/** Create a new (platform specific) writer for the given sink. */
expect public fun IXmlStreaming.newWriter(
    target: Sink,
    repairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.None
): XmlWriter

/** Create a new generic writer for the given sink. */
expect public fun IXmlStreaming.newGenericWriter(
    target: Sink,
    repairNamespaces: Boolean = false,
    xmlDeclMode: XmlDeclMode = XmlDeclMode.None
): XmlWriter
