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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.dom.PlatformDOMImplementation
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Node

/**
 * IXMLStreaming is the interface (accessible through [xmlStreaming]) that exposes the XML parsing
 * and serialization in a platform independent way.
 */
public interface IXmlStreaming {

    @Deprecated("Use the extension method for the JVM platform", level = DeprecationLevel.HIDDEN)
    public fun setFactory(factory: XmlStreamingFactory?): Unit = when (factory) {
        null -> {}// do nothing
        else -> throw UnsupportedOperationException("Setting factories is no longer supported.")
    }

    /**
     * Create a new XML reader with the given input. Depending on the configuration, this parser
     * can be platform specific.
     * @param input The text to be parsed
     * @param expandEntities If true, entities are directly expanded (throwing errors if not found)
     * @return A (potentially platform specific) [XmlReader]
     */
    public fun newReader(input: CharSequence, expandEntities: Boolean = false): XmlReader

    /**
     * Create a new XML reader with the given input. Depending on the configuration, this parser
     * can be platform specific.
     * @param reader The reader/stream to use as input
     * @param expandEntities If true, entities are directly expanded (throwing errors if not found)
     * @return A (potentially platform specific) [XmlReader]
     */
    public fun newReader(reader: Reader, expandEntities: Boolean = false): XmlReader

    /**
     * Create a new XML reader with the given source node as starting point.  Depending on the
     * configuration, this parser can be platform specific.
     * @param source The node to expose
     * @return A (potentially platform specific) [XmlReader], generally a [DomReader]
     */
    @ExperimentalXmlUtilApi
    public fun newReader(source: Node): XmlReader

    /**
     * Create a new XML reader with the given input. This reader is generic.
     * @param input The text to be parsed
     * @param expandEntities Whether entity references should be emitted as distinct events, or
     * treated as simple text. In both cases, entities are resolved and their value available via
     * [XmlReader.text]. Accessing `text` for an unknown entity will throw an exception. If
     * [expandEntities] is false, the name of the entity is available via [XmlReader.localName].
     * @return A platform independent [XmlReader], generally [nl.adaptivity.xmlutil.core.KtXmlReader]
     */
    public fun newGenericReader(input: CharSequence, expandEntities: Boolean = false): XmlReader

    /**
     * Create a new XML reader with the given input. This reader is generic.
     * @param reader The reader/stream to use as input
     * @param expandEntities Whether entity references should be emitted as distinct events, or
     * treated as simple text. In both cases, entities are resolved and their value available via
     * [XmlReader.text]. Accessing `text` for an unknown entity will throw an exception. If
     * [expandEntities] is false, the name of the entity is available via [XmlReader.localName].
     * @return A platform independent [XmlReader], generally [nl.adaptivity.xmlutil.core.KtXmlReader]
     */
    public fun newGenericReader(reader: Reader, expandEntities: Boolean = false): XmlReader

    /**
     * Create a new [DomWriter] that results in writing a DOM tree.
     * @return The [DomWriter]
     */
    @ExperimentalXmlUtilApi
    public fun newWriter(): DomWriter

    /**
     * Create a new [DomWriter] that results in writing to DOM with [dest] as the receiver node.
     * @param dest Destination node that will be the root
     * @return The [DomWriter]
     */
    @ExperimentalXmlUtilApi
    public fun newWriter(dest: Node): DomWriter

    /**
     * Get a DOM implementation that may be platform specific (where available)
     * @return A platform specific DOM implementation
     */
    public val platformDOMImplementation: PlatformDOMImplementation

    /**
     * Get a generic (platform independent) DOM implementation.
     * @return A generic DOM implementation
     */
    public val genericDomImplementation: DOMImplementation
}
