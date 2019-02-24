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

import kotlinx.io.Writer
import org.w3c.dom.Node
import org.w3c.dom.ParentNode
import org.w3c.dom.parsing.DOMParser
import kotlin.reflect.KClass

actual interface XmlStreamingFactory


/**
 * Created by pdvrieze on 13/04/17.
 */
actual object XmlStreaming {


    fun newWriter(): JSDomWriter {
        return JSDomWriter()
    }

    fun newWriter(dest: ParentNode): JSDomWriter {
        return JSDomWriter(dest)
    }


    fun newReader(delegate: Node): JSDomReader {
        return JSDomReader(delegate)
    }

    actual fun setFactory(factory: XmlStreamingFactory?) {
        if (factory!=null)
            throw UnsupportedOperationException("Javascript has no services, don't bother creating them")
    }

    @Suppress("UNUSED_PARAMETER")
    fun <T:Any> deSerialize(input: String, type: KClass<T>): Nothing = TODO("JS does not support annotations")
    /*: T {
        return newReader(input).deSerialize(type)
    }*/

    actual inline fun <reified T:Any> deSerialize(input:String): T = TODO("JS does not support annotations")
    /*: T {
        return deSerialize(input, T::class)
    }*/


    actual fun toString(value: XmlSerializable): String {
        val w = newWriter()
        try {
            value.serialize(w)
        } finally {
            w.close()
        }
        return w.target.toString()
    }

    actual fun newReader(input: CharSequence): XmlReader {
        return JSDomReader(DOMParser().parseFromString(input.toString(), "text/xml"))
    }

    actual fun newWriter(output: Appendable,
                         repairNamespaces: Boolean,
                         omitXmlDecl: Boolean): XmlWriter {
        return AppendingWriter(output, JSDomWriter())
    }

    actual fun newWriter(writer: Writer, repairNamespaces: Boolean, omitXmlDecl: Boolean): XmlWriter {
        return WriterXmlWriter(writer, JSDomWriter())
    }
}

/*
fun <T:Any> JSDomReader.deSerialize(type: KClass<T>): T {
    TODO("Kotlin JS does not support annotations yet so no way to determine the deserializer")
    val an = type.annotations.firstOrNull { jsTypeOf(it) == kotlin.js.jsClass<XmlDeserializer>().name }
    val deserializer = type.getAnnotation(XmlDeserializer::class.java) ?: throw IllegalArgumentException("Types must be annotated with " + XmlDeserializer::class.java.name + " to be deserialized automatically")

    return type.cast(deserializer.value.java.newInstance().deserialize(this))
}
 */

internal class AppendingWriter(private val target: Appendable, private val delegate: JSDomWriter): XmlWriter by delegate {
    override fun close() {
        delegate.close()
        target.append(delegate.toString())
    }

    override fun flush() {
        delegate.flush()
    }
}

internal class WriterXmlWriter(private val target: Writer, private val delegate: JSDomWriter): XmlWriter by delegate {
    override fun close() {
        delegate.close()
        target.write(delegate.toString())
    }

    override fun flush() {
        delegate.flush()
    }
}
