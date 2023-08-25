/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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
import org.w3c.dom.Node
import org.w3c.dom.ParentNode
import org.w3c.dom.parsing.DOMParser
import org.w3c.dom.parsing.XMLSerializer
import kotlin.reflect.KClass

public actual interface XmlStreamingFactory


public actual object XmlStreaming {


    public fun newWriter(): DomWriter {
        return DomWriter()
    }

    public fun newWriter(dest: ParentNode): DomWriter {
        return DomWriter(dest as nl.adaptivity.xmlutil.dom.Node)
    }


    public fun newReader(delegate: Node): DomReader {
        return DomReader(delegate as nl.adaptivity.xmlutil.dom.Node)
    }

    public actual fun setFactory(factory: XmlStreamingFactory?) {
        if (factory != null)
            throw UnsupportedOperationException("Javascript has no services, don't bother creating them")
    }

    @Suppress("UNUSED_PARAMETER")
    public fun <T : Any> deSerialize(input: String, type: KClass<T>): Nothing = TODO("JS does not support annotations")
    /*: T {
        return newReader(input).deSerialize(type)
    }*/

    public actual inline fun <reified T : Any> deSerialize(input: String): T = TODO("JS does not support annotations")
    /*: T {
        return deSerialize(input, T::class)
    }*/

    public actual fun newReader(input: CharSequence): XmlReader {
        val str = when { // Ignore initial BOM (it parses incorrectly without exception)
            input.get(0) == '\ufeff' -> input.subSequence(1, input.length)
            else -> input
        }.toString()
        return DomReader(DOMParser().parseFromString(str, "text/xml") as nl.adaptivity.xmlutil.dom.Node)
    }

    public actual fun newReader(reader: Reader): XmlReader = KtXmlReader(reader)

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
        return AppendingWriter(output, DomWriter(xmlDeclMode))
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
        return WriterXmlWriter(writer, DomWriter(xmlDeclMode))
    }
}

/*
fun <T:Any> DomReader.deSerialize(type: KClass<T>): T {
    TODO("Kotlin JS does not support annotations yet so no way to determine the deserializer")
    val an = type.annotations.firstOrNull { jsTypeOf(it) == kotlin.js.jsClass<XmlDeserializer>().name }
    val deserializer = type.getAnnotation(XmlDeserializer::class.java) ?: throw IllegalArgumentException("Types must be annotated with " + XmlDeserializer::class.java.name + " to be deserialized automatically")

    return type.cast(deserializer.value.java.newInstance().deserialize(this))
}
 */

internal class AppendingWriter(private val target: Appendable, private val delegate: DomWriter) :
    XmlWriter by delegate {
    override fun close() {
        try {
            val xmls = XMLSerializer()
            val domText = xmls.serializeToString(delegate.target as Node)
            target.append(domText)
        } finally {
            delegate.close()
        }
    }

    override fun flush() {
        delegate.flush()
    }
}

internal class WriterXmlWriter(private val target: Writer, private val delegate: DomWriter) : XmlWriter by delegate {
    override fun close() {
        try {
            val xmls = XMLSerializer()
            val domText = xmls.serializeToString(delegate.target as Node)

            val xmlDeclMode = delegate.xmlDeclMode
            if (xmlDeclMode != XmlDeclMode.None) {
                val encoding = when (xmlDeclMode) {
                    XmlDeclMode.Charset -> delegate.requestedEncoding ?: "UTF-8"
                    else -> when (delegate.requestedEncoding?.lowercase()?.startsWith("utf-")) {
                        false -> delegate.requestedEncoding
                        else -> null
                    }
                }

                val xmlVersion = delegate.requestedVersion ?: "1.0"

                target.write("<?xml version=\"")
                target.write(xmlVersion)
                target.write("\"")
                if (encoding != null) {
                    target.write(" encoding=\"")
                    target.write(encoding)
                    target.write("\"")
                }
                target.write("?>")
                if (delegate.indentSequence.isNotEmpty()) {
                    target.write("\n")
                }
            }

            target.write(domText)
        } finally {
            delegate.close()
        }
    }

    override fun flush() {
        delegate.flush()
    }
}
