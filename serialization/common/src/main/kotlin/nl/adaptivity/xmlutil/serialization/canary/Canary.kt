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
import nl.adaptivity.xmlutil.serialization.compat.SerialDescriptor

object Canary {

    private val saverMap = mutableMapOf<KSerialSaver<*>, SerialDescriptor>()
    private val loaderMap = mutableMapOf<KSerialLoader<*>, ExtInfo>()

    private val loaderMap2 = mutableMapOf<KSerialLoader<*>, SerialDescriptor>()

    fun <T> serialDescriptor(saver: KSerialSaver<T>, obj: T): SerialDescriptor {
        val current = saverMap[saver]?.also { return it }
        if (current != null) return current

        val output = OutputCanary((saver as? KSerializer<*>)?.serialClassDesc)
        saver.save(output, obj)
        val new: SerialDescriptor = output.serialDescriptor()
        if(output.isComplete) { // Only save complete descriptors
            saverMap[saver] = new
        }

        return new
    }

    fun <T> serialDescriptor(loader: KSerialLoader<T>): SerialDescriptor {
        val current = loaderMap2[loader]
        if (current != null) return current

        val input = InputCanary()
        load(input, loader)
        val new = input.serialDescriptor()

        if (input.isComplete) {
            loaderMap2[loader] = new
        }

        return new

    }

    fun <T> extInfo(loader: KSerialLoader<T>): ExtInfo {
        val current = loaderMap[loader]
        if (current != null) return current

        val input = CanaryInput()
        load(input, loader)
        val new = input.extInfo()

        if (new.childInfo.none { it.type == ChildType.UNKNOWN }) {
            loaderMap[loader] = new
        }

        return new

    }

    internal fun <T> load(input: CanaryInput,
                          loader: KSerialLoader<T>) {
        try {
            loader.load(input)
        } catch (e: CanaryInput.SuspendException) {
            if (e.finished) {
                return
            }
        }
        while (true) {
            try {
                loader.load(input)
                throw IllegalStateException("This should not be reachable")
            } catch (e: CanaryInput.SuspendException) {
                if (e.finished) break
            } catch (e: UnknownFieldException) {
                throw IllegalStateException("Could not gather information for loader $loader on field ${input.currentChildIndex} with info: ${input.childInfo[input.currentChildIndex]}", e)
            }

        }
    }

    internal fun <T> load(input: InputCanary,
                          loader: KSerialLoader<T>) {
        try {
            loader.load(input)
        } catch (e: InputCanary.SuspendException) {
            if (e.finished) {
                return
            }
        }
        while (true) {
            try {
                loader.load(input)
                throw IllegalStateException("This should not be reachable")
            } catch (e: InputCanary.SuspendException) {
                if (e.finished) break
            } catch (e: UnknownFieldException) {
                throw IllegalStateException("Could not gather information for loader $loader on field ${input.currentChildIndex} with info: ${input.childDescriptors[input.currentChildIndex]}", e)
            }

        }
    }

    fun <T> pollInfo(loader: KSerialLoader<T>): ExtInfo? {
        return loaderMap[loader]
    }

    fun <T> pollDesc(saver: KSerialSaver<T>): SerialDescriptor? {
        return saverMap[saver]
    }

    internal fun registerDesc(saver: KSerialSaver<*>, desc: SerialDescriptor) {
        saverMap[saver] = desc
    }

    internal fun registerDesc(loader: KSerialLoader<*>, desc: SerialDescriptor) {
        loaderMap2[loader] = desc
    }

    fun <T> pollDesc(loader: KSerialLoader<T>): SerialDescriptor? {
        return loaderMap2[loader]
    }

    internal fun childInfoForClassDesc(desc: KSerialClassDesc): Array<OldChildInfo> {
        return when (desc.kind) {
            KSerialClassKind.MAP,
            KSerialClassKind.SET,
            KSerialClassKind.LIST
                 -> arrayOf(OldChildInfo("count", classAnnotations = emptyList()), OldChildInfo("values", classAnnotations = emptyList()))

            else -> Array(desc.associatedFieldsCount) {
                OldChildInfo(desc.getElementName(it), desc.getAnnotationsForIndex(it), emptyList())
            }
        }
    }


}