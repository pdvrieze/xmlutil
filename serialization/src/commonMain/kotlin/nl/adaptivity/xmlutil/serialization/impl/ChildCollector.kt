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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleCollector
import kotlin.reflect.KClass

@ExperimentalSerializationApi
internal class ChildCollector constructor(private val wantedBaseClass: KClass<*>? = null) : SerializersModuleCollector {
    internal val children = mutableListOf<KSerializer<*>>()

    override fun <Base : Any> polymorphicDefault(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (className: String?) -> DeserializationStrategy<out Base>?
                                                ) {
        // ignore
    }

    override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
        // ignore
    }

    override fun <T : Any> contextual(
        kClass: KClass<T>,
        provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>
                                     ) {
        // ignore
    }

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
                                                     ) {
        if (wantedBaseClass== null || wantedBaseClass == baseClass) {
            children.add(actualSerializer)
        }
    }

}
