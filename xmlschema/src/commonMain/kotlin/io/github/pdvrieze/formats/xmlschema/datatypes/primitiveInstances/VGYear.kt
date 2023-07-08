/*
 * Copyright (c) 2023.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.prefix
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class VGYear(val year: Int) : VAnyAtomicType {
    override val xmlString: String get() = year.toString().padStart(4, '0')
}

@JvmInline
@Serializable
value class VNotation(val value: SerializableQName) : VAnyAtomicType {

    constructor(str: VString) : this((str as? VPrefixString)?.toQName() ?: QName(str.xmlString))

    override val xmlString: String get() = "${value.prefix}:${value.localPart}"
}
