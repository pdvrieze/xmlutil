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
import kotlinx.serialization.PolymorphicKind
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerialModuleCollector
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlCodecBase.Companion.declRequestedName
import nl.adaptivity.xmlutil.serialization.XmlNameMap
import nl.adaptivity.xmlutil.serialization.canary.polyBaseClassName
import nl.adaptivity.xmlutil.toNamespace
import kotlin.reflect.KClass

internal class ChildCollector private constructor(val matcher: Matcher) : SerialModuleCollector {
    internal val children = mutableListOf<KSerializer<*>>()

    override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
        // ignore
    }

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
                                                     ) {
        if (matcher(baseClass, actualClass, actualSerializer)) {
            children.add(actualSerializer)
        }
    }

    /**
     * Get the polymorphic information for the found children.
     */
    fun getPolyInfo(parentTagName: QName): XmlNameMap = XmlNameMap().apply {
        for (actualSerializer in children) {
            val declName = actualSerializer.descriptor.declRequestedName(parentTagName.toNamespace())

            // The class is always treated as specified in automatic polymorphic mode. It should never use the field
            // name as that cannot be correct.
            val nameInSerializer = actualSerializer.descriptor.serialName
            registerClass(declName, nameInSerializer, actualSerializer, true)
        }
    }

    internal data class ActualChildInfo<T : Any>(val actualClass: KClass<T>, val actualSerializer: KSerializer<T>)

    internal interface Matcher {
        operator fun <Base : Any, Sub : Any> invoke(
            baseClass: KClass<Base>,
            actualClass: KClass<Sub>,
            actualSerializer: KSerializer<Sub>
                                          ): Boolean
    }

    private class KlassMatcher(private val baseClass: KClass<*>): Matcher {
        override operator fun <Base : Any, Sub : Any> invoke(
            baseClass: KClass<Base>,
            actualClass: KClass<Sub>,
            actualSerializer: KSerializer<Sub>
                                                   ): Boolean {
            return baseClass==this.baseClass
        }
    }

    private class KlassNameMatcher(private val baseClassName: String): Matcher {
        override operator fun <Base : Any, Sub : Any> invoke(
            baseClass: KClass<Base>,
            actualClass: KClass<Sub>,
            actualSerializer: KSerializer<Sub>
                                                   ): Boolean {
            return baseClass.simpleName==baseClassName
        }
    }

    companion object {
        operator fun invoke(baseClass: KClass<*>) = ChildCollector(KlassMatcher(baseClass))
        operator fun invoke(baseClassName: String) = ChildCollector(KlassNameMatcher(baseClassName))
    }

}

internal class KClassCollector constructor(val kClassName: String) : SerialModuleCollector {
    internal var kClass: KClass<*>? = null
        private set

    override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
        if (this.kClass ==null && kClass.simpleName==kClassName) {
            this.kClass = kClass
        }
        // ignore
    }

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
                                                     ) {
        when {
            kClass != null -> return
            baseClass.simpleName == kClassName -> kClass = baseClass
            actualClass.simpleName == kClassName -> kClass = baseClass
        }
    }

}

public fun SerialModule.getPolymorphic(baseClassName: String, serializedClassName: String): KSerializer<*>? {
    val collector = ChildCollector(baseClassName)
    dumpTo(collector)

    return collector.children.firstOrNull { it.descriptor.serialName == serializedClassName }
}


// TODO on kotlinx.serialization-1.0 use the actual information provided
internal fun SerialDescriptor.capturedKClass(context: SerialModule): KClass<*>? {
    val baseClassName = when(kind) {
        PolymorphicKind.SEALED,
        PolymorphicKind.OPEN -> this.polyBaseClassName ?: Any::class.simpleName!!
        else -> return null
    }

    return KClassCollector(baseClassName).also { context.dumpTo(it) }.kClass
}
