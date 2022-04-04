/*
 * Copyright (c) 2022.
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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import java.net.URI
import java.net.URL

internal class SimpleResolver(private val baseURI: URI): ResolvedSchema.Resolver {
    override fun readSchema(schemaLocation: VAnyURI): XSSchema {
        return baseURI.resolve(schemaLocation.value).withXmlReader { reader ->
            return XML { autoPolymorphic = true }.decodeFromReader<XSSchema>(reader)
        }

    }
}


private inline fun <R> URI.withXmlReader(body: (XmlReader)->R): R {
    return toURL().withXmlReader(body)
}

private inline fun <R> URL.withXmlReader(body: (XmlReader) -> R): R {
    return openStream().use { inStream ->
        XmlStreaming.newReader(inStream, "UTF-8").use(body)
    }
}
