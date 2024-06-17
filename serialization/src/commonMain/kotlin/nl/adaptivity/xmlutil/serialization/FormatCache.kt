/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.serialization.structure.XmlTypeDescriptor

/**
 * Opaque caching class that allows for caching format related data (to speed up reuse). This is
 * intended to be stored on the config, thus reused through multiple serializations.
 * Note that this requires the `serialName` attribute of `SerialDescriptor` instances to be unique.
 */
public class FormatCache {
    private val cache = mutableMapOf<QName, XmlTypeDescriptor>()

    internal inline fun lookupType(namespace: Namespace?, serialName: String, kind: SerialKind, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor {
        return lookupType(QName(namespace?.namespaceURI ?: "", serialName), kind, defaultValue)
    }

    internal inline fun lookupType(
        parentName: QName,
        serialName: String,
        kind: SerialKind,
        defaultValue: () -> XmlTypeDescriptor
    ): XmlTypeDescriptor {
        return lookupType(QName(parentName.namespaceURI, serialName), kind, defaultValue)
    }

    internal inline fun lookupType(namespace: String, serialName: String, kind: SerialKind, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor {
        return lookupType(QName(namespace, serialName), kind, defaultValue)
    }

    internal inline fun lookupType(namespace: Namespace?, serialDesc: SerialDescriptor, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor {
        return lookupType(QName(namespace?.namespaceURI ?: "", serialDesc.serialName), serialDesc.kind, defaultValue)
    }

    internal inline fun lookupType(parentName: QName, serialDesc: SerialDescriptor, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor {
        return lookupType(QName(parentName.namespaceURI, serialDesc.serialName), serialDesc.kind, defaultValue)
    }

    internal inline fun lookupType(namespace: String, serialDesc: SerialDescriptor, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor {
        return lookupType(QName(namespace, serialDesc.serialName), serialDesc.kind, defaultValue)
    }

    internal inline fun lookupType(name: QName, kind: SerialKind, defaultValue: () -> XmlTypeDescriptor): XmlTypeDescriptor {
        return when (kind) {
            StructureKind.MAP,
            StructureKind.LIST -> defaultValue()

            else -> cache.getOrPut(name, defaultValue)
        }
    }
}
