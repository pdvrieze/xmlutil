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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.UnknownFieldException
import nl.adaptivity.xmlutil.serialization.XmlSerialException
import kotlin.collections.mutableMapOf
import kotlin.collections.set

object Canary {

    private val saverMap = mutableMapOf<SerializationStrategy<*>, ExtSerialDescriptor>()

    private val loaderMap = mutableMapOf<DeserializationStrategy<*>, ExtSerialDescriptor>()

    fun <T> serialDescriptor(saver: SerializationStrategy<T>, obj: T): ExtSerialDescriptor {
        val current = saverMap[saver]?.also { return it }
        if (current != null) return current

        val output = OutputCanary((saver as? KSerializer<*>)?.descriptor)
        saver.serialize(output, obj)
        val new: ExtSerialDescriptor = output.serialDescriptor()
        if(output.isComplete) { // Only save complete descriptors
            saverMap[saver] = new
        }

        return new
    }

    fun <T> serialDescriptor(loader: DeserializationStrategy<T>): ExtSerialDescriptor {
        val current = loaderMap[loader]
        if (current != null) return current

        val input = InputCanary()
        load(input, loader)
        val new = input.serialDescriptor()

        if (input.isComplete) {
            loaderMap[loader] = new
        }

        return new

    }

    internal fun <T> load(input: InputCanary,
                          loader: DeserializationStrategy<T>
                         ) {
        try {
            loader.deserialize(input)
        } catch (e: InputCanary.SuspendException) {
            if (e.finished) {
                return
            }
        }
        while (true) {
            try {
                loader.deserialize(input)
                throw AssertionError("This should not be reachable")
            } catch (e: InputCanary.SuspendException) {
                if (e.finished) break
            } catch (e: UnknownFieldException) {
                throw XmlSerialException("Could not gather information for loader $loader on field ${input.currentChildIndex} with info: ${input.childDescriptors[input.currentChildIndex]}", e)
            }

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