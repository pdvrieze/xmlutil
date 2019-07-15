/*
 * Copyright (c) 2019.
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

package nl.adaptivity.xmlutil.serialization.impl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerialModuleCollector
import nl.adaptivity.serialutil.impl.name
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.PolyInfo
import nl.adaptivity.xmlutil.serialization.XmlCodecBase
import nl.adaptivity.xmlutil.serialization.XmlNameMap
import nl.adaptivity.xmlutil.toNamespace
import kotlin.reflect.KClass

internal class ChildCollector(val baseClass: KClass<*>) : SerialModuleCollector {
    internal val children = mutableListOf<ActualChildInfo<*>>()

    override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
        // ignore
    }

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
                                                     ) {
        if (baseClass == this.baseClass) {
            children.add(ActualChildInfo(actualClass, actualSerializer))
        }
    }

    fun getPolyInfo(codec: XmlCodecBase.XmlTagCodec, parentTagName: QName): XmlNameMap? = when (children.size) {
        0    -> null
        else -> XmlNameMap().apply {
            for ((actualClass, actualSerializer) in children) {
                val declName = with(XmlCodecBase) { actualSerializer.descriptor.declRequestedName(parentTagName.toNamespace()) }
                val polyInfo = PolyInfo(actualClass.name, declName, -1, actualSerializer)

                // The class is always treated as specified in automatic polymorphic mode. It should never use the field
                // name as that cannot be correct.
                registerClass(declName, actualClass.name, actualSerializer, true)
            }
        }
    }

    internal data class ActualChildInfo<T : Any>(val actualClass: KClass<T>, val actualSerializer: KSerializer<T>)

}