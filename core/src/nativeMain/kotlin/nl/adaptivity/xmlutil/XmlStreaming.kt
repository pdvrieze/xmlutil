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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer

/**
 * This class is the entry point for creating [XmlReader] and [XmlWriter]
 * instances. Some interfaces are common, others are limited to some
 * architectures.
 */
public actual object XmlStreaming {

    public actual fun setFactory(factory: XmlStreamingFactory?) {
        throw UnsupportedOperationException("Native does not support setting the factory")
    }

    public actual inline fun <reified T : Any> deSerialize(input: String): T {
        throw UnsupportedOperationException("Cannot work")
    }

    public actual fun newReader(input: CharSequence): XmlReader {
        return KtXmlReader(StringReader(input))
    }

    public actual fun newReader(reader: Reader): XmlReader {
        return newGenericReader(reader)
    }

    public actual fun newGenericReader(input: CharSequence): XmlReader =
        newGenericReader(StringReader(input))

    public actual fun newGenericReader(reader: Reader): XmlReader = KtXmlReader(reader)

    public actual fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean,
        omitXmlDecl: Boolean
    ): XmlWriter {
        return newWriter(output, repairNamespaces, XmlDeclMode.from(omitXmlDecl))
    }

    public actual fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
    ): XmlWriter {
        return KtXmlWriter(output, repairNamespaces, xmlDeclMode)
    }

    public actual fun newGenericWriter(
        output: Appendable,
        isRepairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode
    ): KtXmlWriter {
        return KtXmlWriter(output, isRepairNamespaces, xmlDeclMode)
    }

    public actual fun newWriter(writer: Writer, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter {
        return newWriter(writer, repairNamespaces, XmlDeclMode.from(omitXmlDecl))
    }

    public actual fun newWriter(writer: Writer, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode): XmlWriter {
        return KtXmlWriter(writer, repairNamespaces, xmlDeclMode)
    }

}
