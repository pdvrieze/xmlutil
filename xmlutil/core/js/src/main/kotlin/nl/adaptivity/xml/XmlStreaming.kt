/*
 * Copyright (c) 2017.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import org.w3c.dom.Element
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
        throw UnsupportedOperationException("Javascript has no services, don't bother creating them")
    }

    fun <T:Any> deSerialize(input: String, type: KClass<T>): T {
        val document = DOMParser().parseFromString(input, "text/xml")
        val reader = JSDomReader(document)
        return reader.deSerialize(type)
    }

    actual inline fun <reified T:Any> deSerialize(input:String): T {
        return deSerialize(input, T::class)
    }


    actual fun toString(value: XmlSerializable): String {
        val w = XmlStreaming.newWriter()
        try {
            value.serialize(w)
        } finally {
            w.close()
        }
        return w.target.toString()
    }
}


fun <T:Any> JSDomReader.deSerialize(type: KClass<T>): T {
    TODO("Kotlin JS does not support annotations yet so no way to determine the deserializer")
/*
    val an = type.annotations.firstOrNull { jsTypeOf(it) == kotlin.js.jsClass<XmlDeserializer>().name }
    val deserializer = type.getAnnotation(XmlDeserializer::class.java) ?: throw IllegalArgumentException("Types must be annotated with " + XmlDeserializer::class.java.name + " to be deserialized automatically")

    return type.cast(deserializer.value.java.newInstance().deserialize(this))
*/
}
