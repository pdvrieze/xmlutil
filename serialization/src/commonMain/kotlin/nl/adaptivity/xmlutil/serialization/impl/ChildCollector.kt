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
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlCodecBase
import nl.adaptivity.xmlutil.serialization.XmlCodecBase.Companion.declRequestedName
import nl.adaptivity.xmlutil.serialization.XmlNameMap
import nl.adaptivity.xmlutil.toNamespace
import kotlin.reflect.KClass

internal class ChildCollector(val baseClass: KClass<*>) : SerialModuleCollector {
    internal val children = mutableListOf<KSerializer<*>>()

    override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
        // ignore
    }

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
                                                     ) {
        if (baseClass == this.baseClass) {
            children.add(actualSerializer)
        }
    }

    /**
     * Get the polymorphic information for the found children.
     */
    fun getPolyInfo(parentTagName: QName): XmlNameMap? = when (children.size) {
        0    -> null
        else -> XmlNameMap().apply {
            for (actualSerializer in children) {
                val declName = actualSerializer.descriptor.declRequestedName(parentTagName.toNamespace())

                // The class is always treated as specified in automatic polymorphic mode. It should never use the field
                // name as that cannot be correct.
                val nameInSerializer = actualSerializer.descriptor.serialName
                registerClass(declName, nameInSerializer, actualSerializer, true)
            }
        }
    }

    internal data class ActualChildInfo<T : Any>(val actualClass: KClass<T>, val actualSerializer: KSerializer<T>)

}