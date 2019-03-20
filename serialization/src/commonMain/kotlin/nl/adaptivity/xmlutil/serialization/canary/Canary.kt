/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.*
import kotlinx.serialization.internal.GeneratedSerializer
import kotlinx.serialization.internal.MissingDescriptorException
import nl.adaptivity.util.kotlin.xmlutilserialization.arrayMap
import nl.adaptivity.xmlutil.serialization.XmlSerialException
import kotlin.collections.mutableMapOf
import kotlin.collections.set

object Canary {

    private val saverMap = mutableMapOf<SerializationStrategy<*>, ExtSerialDescriptor>()

    private val loaderMap = mutableMapOf<DeserializationStrategy<*>, ExtSerialDescriptor>()

    fun <T> serialDescriptor(saver: SerializationStrategy<T>, obj: T): ExtSerialDescriptor {
        val current = saverMap[saver]?.also { return it }
        if (current != null) return current
        if (saver is GeneratedSerializer) {
            return ExtSerialDescriptorImpl(saver.descriptor,
                                           saver.childSerializers().arrayMap { it.descriptor }).also { saverMap[saver] = it }
        }
        val parentDesc = saver.descriptor
        try {
            val childDescs = Array(parentDesc.elementsCount) { parentDesc.getElementDescriptor(it) }
            return ExtSerialDescriptorImpl(parentDesc, childDescs).also { saverMap[saver] = it }
        } catch (e: MissingDescriptorException) {
            return ExtSerialDescriptorImpl(parentDesc, emptyArray()).also { saverMap[saver] = it }
        }
    }

    fun <T> serialDescriptor(loader: DeserializationStrategy<T>): ExtSerialDescriptor {
        val current = loaderMap[loader]
        if (current != null) return current
        if (loader is GeneratedSerializer) {
            return ExtSerialDescriptorImpl(loader.descriptor,
                                           loader.childSerializers().arrayMap { it.descriptor }).also { loaderMap[loader] = it }
        }
        val parentDesc = loader.descriptor
        try {
            val childDescs = Array(parentDesc.elementsCount) { parentDesc.getElementDescriptor(it) }
            return ExtSerialDescriptorImpl(parentDesc, childDescs).also { loaderMap[loader] = it }
        } catch (e: MissingDescriptorException) {
            return ExtSerialDescriptorImpl(parentDesc, emptyArray()).also { loaderMap[loader] = it }
        }
    }

    fun <T> pollDesc(saver: SerializationStrategy<T>): ExtSerialDescriptor? {
        return saverMap[saver]
    }

    internal fun registerDesc(saver: SerializationStrategy<*>, desc: ExtSerialDescriptor) {
        saverMap[saver] = desc
    }

    internal fun registerDesc(loader: DeserializationStrategy<*>, desc: ExtSerialDescriptor) {
        loaderMap[loader] = desc
    }

    fun <T> pollDesc(loader: DeserializationStrategy<T>): ExtSerialDescriptor? {
        return loaderMap[loader]
    }


}