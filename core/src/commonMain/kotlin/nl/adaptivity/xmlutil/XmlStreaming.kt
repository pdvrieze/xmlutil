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

/**
 * Utility class with factories and constants for the [XmlReader] and [XmlWriter] interfaces.
 * Created by pdvrieze on 15/11/15.
 */
expect object XmlStreaming {

    fun setFactory(factory: XmlStreamingFactory?)

    inline fun <reified T : Any> deSerialize(input: String): T

    fun toString(value: XmlSerializable): String

    fun newReader(input: CharSequence): XmlReader

    fun newWriter(output: Appendable, repairNamespaces: Boolean = false, omitXmlDecl: Boolean = false): XmlWriter

    fun newWriter(writer: Writer, repairNamespaces: Boolean, omitXmlDecl: Boolean = false): XmlWriter
}

