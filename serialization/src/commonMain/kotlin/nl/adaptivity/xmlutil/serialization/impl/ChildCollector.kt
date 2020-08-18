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
import nl.adaptivity.xmlutil.serialization.impl.polyBaseClassName
import kotlin.reflect.KClass

internal class ChildCollector constructor(private val wantedBaseClass: KClass<*>) : SerialModuleCollector {
    internal val children = mutableListOf<KSerializer<*>>()

    override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
        // ignore
    }

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
                                                     ) {
        if (wantedBaseClass == baseClass) {
            children.add(actualSerializer)
        }
    }

}

/**
 * Used by capturedKClass to capture the kClass while we don't have proper capturedClass support.
 */
private class KClassCollector constructor(val kClassName: String) : SerialModuleCollector {
    var kClass: KClass<*>? = null
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


// TODO on kotlinx.serialization-1.0 use the actual information provided
internal fun SerialDescriptor.capturedKClass(context: SerialModule): KClass<*>? {
    val baseClassName = when(kind) {
        PolymorphicKind.SEALED,
        PolymorphicKind.OPEN -> this.polyBaseClassName ?: Any::class.simpleName!!
        else -> return null
    }

    return KClassCollector(baseClassName).also { context.dumpTo(it) }.kClass
}

private val SerialDescriptor.polyBaseClassName: String?
    get() {
        val valueName = getElementDescriptor(1).serialName
        val startIdx = valueName.indexOf('<')
        if (startIdx>=0 && valueName.endsWith(">")) {
            return valueName.substring(startIdx + 1, valueName.length - 1)
        } else {
            return null
        }

    }