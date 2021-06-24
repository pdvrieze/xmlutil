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

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

internal fun serializerByTypeInfo(type: KType): KSerializer<*> {
    val classifierClass = type.classifier as? KClass<*>
    if (classifierClass != null && classifierClass.java.isArray) {
        return arraySerializer(type)
    }

    return serializer(type)
}

// NOTE: this should be removed once kotlinx.serialization serializer get support of arrays that is blocked by KT-32839
@OptIn(ExperimentalSerializationApi::class)
private fun arraySerializer(type: KType): KSerializer<*> {
    val elementType = type.arguments[0].type ?: error("Array<*> is not supported")
    val elementSerializer = serializerByTypeInfo(elementType)

    @Suppress("UNCHECKED_CAST")
    return ArraySerializer(
        elementType.jvmErasure as KClass<Any>,
        elementSerializer as KSerializer<Any>
                          )
}

internal fun serializerFromResponseType(
    context: PipelineContext<Any, ApplicationCall>,
    module: SerializersModule
                                       ): KSerializer<*>? {
    val responseType = context.call.response.responseType ?: return null
    return module.serializer(responseType)
}

@OptIn(ExperimentalSerializationApi::class)
internal fun guessSerializer(
    value: Any,
    module: SerializersModule
                            ): KSerializer<*> {
    return when (value) {
        is JsonElement     -> JsonElement.serializer()
        is List<*>         -> ListSerializer(value.elementSerializer(module))
        is Set<*>          -> SetSerializer(value.elementSerializer(module))
        is Map<*, *>       -> MapSerializer(
            value.keys.elementSerializer(module),
            value.values.elementSerializer(module)
                                           )
        is Map.Entry<*, *> -> MapEntrySerializer(
            guessSerializer(value.key ?: error("Map.Entry(null, ...) is not supported"), module),
            guessSerializer(value.value ?: error("Map.Entry(..., null) is not supported)"), module)
                                                )
        is Array<*>        -> {
            val componentType = value.javaClass.componentType.kotlin.starProjectedType
            val componentClass =
                componentType.classifier as? KClass<*> ?: error("Unsupported component type $componentType")

            @Suppress("UNCHECKED_CAST")
            (ArraySerializer(
                componentClass as KClass<Any>,
                serializerByTypeInfo(componentType) as KSerializer<Any>
                            ))
        }
        else               -> module.getContextual(value::class)
            ?: @OptIn(InternalSerializationApi::class) value::class.serializer()
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun Collection<*>.elementSerializer(module: SerializersModule): KSerializer<*> {
    val serializers = mapNotNull { value ->
        value?.let { guessSerializer(it, module) }
    }.distinctBy { it.descriptor.serialName }

    if (serializers.size > 1) {
        val message = "Serializing collections of different element types is not yet supported. " +
                "Selected serializers: ${serializers.map { it.descriptor.serialName }}"
        error(message)
    }

    val selected: KSerializer<*> = serializers.singleOrNull() ?: String.serializer()
    if (selected.descriptor.isNullable) {
        return selected
    }

    @Suppress("UNCHECKED_CAST")
    selected as KSerializer<Any>

    if (any { it == null }) {
        return selected.nullable
    }

    return selected
}
