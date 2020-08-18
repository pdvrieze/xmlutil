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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.modules.SerialModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import kotlin.reflect.KClass

internal abstract class XmlCodecBase internal constructor(
    val context: SerialModule,
    val config: XmlConfig
                                                         ) {

    protected abstract val namespaceContext: NamespaceContext

    /**
     * Determine the polymorphic tag name for a particular element.
     */
    fun polyTagName(
        parent: XmlSerializationPolicy.NameInfo,
        polyChild: String,
        baseClass: KClass<*>
                   ): PolyBaseInfo {

        val currentPkg = parent.serialName.substringBeforeLast('.', "")
        val parentTag = parent.annotatedName!!
        val eqPos = polyChild.indexOf('=')
        val pkgPos: Int
        val prefPos: Int
        val typeNameBase: String
        val prefix: String
        val localPart: String

        if (eqPos < 0) {
            typeNameBase = polyChild
            pkgPos = polyChild.lastIndexOf('.')
            prefPos = -1
            prefix = parentTag.prefix
            localPart = if (pkgPos < 0) polyChild else polyChild.substring(pkgPos + 1)
        } else {
            typeNameBase = polyChild.substring(0, eqPos).trim()
            pkgPos = polyChild.lastIndexOf('.', eqPos - 1)
            prefPos = polyChild.indexOf(':', eqPos + 1)

            if (prefPos < 0) {
                prefix = parentTag.prefix
                localPart = polyChild.substring(eqPos + 1).trim()
            } else {
                prefix = polyChild.substring(eqPos + 1, prefPos).trim()
                localPart = polyChild.substring(prefPos + 1).trim()
            }
        }

        val ns = if (prefPos >= 0) namespaceContext.getNamespaceURI(prefix)
            ?: parentTag.namespaceURI else parentTag.namespaceURI

        val typename = when {
            pkgPos != 0 || currentPkg.isEmpty()
                 -> typeNameBase

            else -> "$currentPkg.${typeNameBase.substring(1)}"
        }

        val descriptor = context.getPolymorphic(baseClass, typename)?.descriptor
            ?: throw XmlException("Missing descriptor for $typename in the serial context")

        val name: QName = when {
            eqPos < 0 -> descriptor.declRequestedName(XmlEvent.NamespaceImpl(prefix, ns))
            else      -> QName(ns, localPart, prefix)
        }

        return PolyBaseInfo(name, -1, descriptor)
    }


    companion object {

        internal fun SerialDescriptor.declRequestedName(parentNamespace: Namespace): QName {
            annotations.firstOrNull<XmlSerialName>()?.let { return it.toQName() }
            return serialName.substringAfterLast('.').toQname(parentNamespace)
        }

        /**
         * TODO: move to policy
         * This function is used by the decoder to try to expand a shortened type name. It is the
         * opposite of [tryShortenTypeName].
         */
        internal fun String.expandTypeNameIfNeeded(parentType: String): String {
            if (!startsWith('.')) return this
            val parentPkg = parentType.lastIndexOf('.').let { idx ->
                if (idx < 0) return substring(1)
                parentType.substring(0, idx)
            }
            return "$parentPkg$this"
        }

        /**
         * TODO: move to policy
         * This function is used by the encoder to try shorten a type name. It is the
         * opposite of [tryShortenTypeName].
         */
        internal fun String.tryShortenTypeName(parentType: String): String {
            val parentPkg = parentType.lastIndexOf('.').let { idx ->
                if (idx < 0) return this
                parentType.substring(0, idx)
            }
            if (startsWith(parentPkg) && indexOf('.', parentPkg.length + 1) < 0) {
                return substring(parentPkg.length) // include starting . to signal relative type
            }
            return this
        }
    }

    @Suppress("RedundantInnerClassModifier") // The actual children must be inner
    abstract inner class XmlCodec<out D : XmlDescriptor>(
        protected val xmlDescriptor: D
                                                        ) {
        val serialName: QName get() = xmlDescriptor.tagName
    }

    internal abstract inner class XmlTagCodec<out D : XmlDescriptor>(val xmlDescriptor: D) {

        val parentDesc get() = xmlDescriptor.tagParent.descriptor.serialDescriptor
        val elementIndex get() = xmlDescriptor.tagParent.index

        internal val config get() = this@XmlCodecBase.config
        val context: SerialModule get() = this@XmlCodecBase.context

        val serialName: QName get() = xmlDescriptor.tagName

        protected abstract val namespaceContext: NamespaceContext

        // TODO it is not clear that the handling of empty namespace is correct
        internal fun QName.normalize(): QName {
            return when {
                namespaceURI.isEmpty() -> copy(
                    namespaceURI = namespaceContext.getNamespaceURI(prefix) ?: "",
                    prefix = ""
                                              )
                else                   -> copy(prefix = "")
            }
        }

    }

}
