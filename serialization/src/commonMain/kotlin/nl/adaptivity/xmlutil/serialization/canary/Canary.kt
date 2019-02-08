/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.*
import kotlinx.serialization.internal.GeneratedSerializer
import kotlinx.serialization.internal.MissingDescriptorException
import nl.adaptivity.util.kotlin.arrayMap
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
            return ExtSerialDescriptorImpl(saver.descriptor, saver.childSerializers().arrayMap { it.descriptor }).also { saverMap[saver] = it }
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
            return ExtSerialDescriptorImpl(loader.descriptor, loader.childSerializers().arrayMap { it.descriptor }).also { loaderMap[loader] = it }
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