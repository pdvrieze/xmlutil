/*
 * Copyright (c) 2020.
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

package net.devrieze.serialization.examples.soap

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * The Envelope class is a very simple implementation of the SOAP Envelope (ignoring existence of headers). The
 * `@XmlSerialName` annotation specifies how the class is to be serialized, including namespace and prefix to try to use
 * (the serializer will try to reuse an existing prefix for the namespace if it already exists in the document).
 *
 * @property body `body` is a property that contains the body of the envelope. It merely wraps the data, but needs to exist
 *      for the purpose of generating the tag.
 * @param BODYTYPE SOAP is a generic protocol and the wrappers should not depend on a particular body data. That is why
 *      the type is parameterized (this works fine with Serialization).
 */
@Serializable
@XmlSerialName("Envelope", "http://schemas.xmlsoap.org/soap/envelope/", "S")
class Envelope<BODYTYPE> private constructor(
    private val body: Body<BODYTYPE>
) {

    /**
     * Actual constructor so users don't need to know about the body element
     */
    constructor(data: BODYTYPE) : this(Body(data))

    /**
     * Accessor to the data property that hides the body element.
     */
    val data: BODYTYPE get() = body.data

    override fun toString(): String {
        return "Envelope(body=$body)"
    }

    /**
     * The body class merely wraps a data element (the SOAP standard requires this to be a single element). There is no
     * need for this type to specify the serial name explicitly because:
     *  1. Body is a class, thus serialized as element. The name used is therefore (by default) determined by the name
     *     of the type (`Body`).
     *  2. The namespace (and prefix) used for a type is by default the namespace of the containing tag.
     *  3. Package names are normally elided in the naming
     *
     * The content of data is polymorphic to allow for different message types.
     *
     * @property data The data property contains the actual message content for the soap message.
     */
    @Serializable
    private data class Body<BODYTYPE>(@Polymorphic val data: BODYTYPE)

}
