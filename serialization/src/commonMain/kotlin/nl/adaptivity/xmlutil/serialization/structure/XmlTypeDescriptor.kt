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

package nl.adaptivity.xmlutil.serialization.structure

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo

public class XmlTypeDescriptor internal constructor(public val serialDescriptor: SerialDescriptor) {

    @OptIn(ExperimentalSerializationApi::class)
    public val typeNameInfo: DeclaredNameInfo = serialDescriptor.getNameInfo()

    @OptIn(ExperimentalSerializationApi::class)
    public val serialName: String
        get() = serialDescriptor.serialName

    public val typeQname: QName? = typeNameInfo.annotatedName

    @OptIn(ExperimentalSerializationApi::class)
    public val elementsCount: Int
        get() = serialDescriptor.elementsCount

    private val children by lazy {
        @OptIn(ExperimentalSerializationApi::class)
        Array(serialDescriptor.elementsCount) {
            XmlTypeDescriptor(serialDescriptor)
        }
    }
}
