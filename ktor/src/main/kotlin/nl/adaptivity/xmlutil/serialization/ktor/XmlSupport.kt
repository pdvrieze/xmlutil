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

package nl.adaptivity.xmlutil.serialization.ktor

import io.ktor.features.*
import io.ktor.http.*
import io.ktor.serialization.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy


/**
 * The default json configuration used in [SerializationConverter]. The settings are:
 * - defaults are serialized
 * - mode is not strict so extra json fields are ignored
 * - pretty printing is disabled
 * - array polymorphism is enabled
 * - keys and values are quoted, non-quoted are not allowed
 *
 * See [Json] for more details.
 */
public val DefaultXml: XML = XML {
    policy = DefaultXmlSerializationPolicy(
        pedantic = false,
        autoPolymorphic = true,
        encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.ANNOTATED
    )
}

/**
 * Register `application/json` (or another specified [contentType]) content type
 * to [ContentNegotiation] feature using kotlinx.serialization.
 *
 * @param json format instance (optional)
 * @param contentType to register with, application/json by default
 */
@OptIn(ExperimentalSerializationApi::class)
public fun ContentNegotiation.Configuration.xml(
    xml: XML = DefaultXml,
    contentType: ContentType = ContentType.Application.Xml
) {
    register(contentType, XmlStreamConverter(xml))
}
