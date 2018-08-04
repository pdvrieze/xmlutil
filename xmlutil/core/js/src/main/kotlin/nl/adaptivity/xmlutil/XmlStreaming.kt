/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil

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
