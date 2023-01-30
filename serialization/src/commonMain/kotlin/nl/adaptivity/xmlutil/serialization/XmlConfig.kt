/*
 * Copyright (c) 2020.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import nl.adaptivity.xmlutil.core.internal.countIndentedLength
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.XmlEncodeDefault
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import kotlin.jvm.JvmOverloads

/**
 * Configuration for the xml parser.
 *
 * @property repairNamespaces Should namespaces automatically be repaired. This option will be passed on to the [XmlWriter]
 * @property omitXmlDecl Should the generated XML contain an XML declaration or not. This is passed to the [XmlWriter]
 * @property indentString The indentation to use. This is passed to the [XmlWriter]. Note that at this point no validation
 *           of the indentation is done, if it is not valid whitespace it will produce unexpected XML.
 * @property indent The indentation level (in spaces) to use. This is derived from [indentString]. Tabs are counted as 8
 *                  characters, everything else as 1
 * @property policy The policy allows for dynamic configuration of the creation of the XML tree that represents
 *                  the serialized format.
 */
public class XmlConfig
@OptIn(ExperimentalSerializationApi::class)
private constructor(
    public val repairNamespaces: Boolean = true,
    public val xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
    public val indentString: String = "",
    public val policy: XmlSerializationPolicy,
    public val nilAttribute: Pair<QName, String>? = null,
    public val xmlVersion: XmlVersion = XmlVersion.XML11
) {

    @ExperimentalXmlUtilApi
    @Deprecated("Use the builder constructor that allows for ABI-safe construction with new parameters")
    public constructor(
        repairNamespaces: Boolean = true,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
        indentString: String = "",
        autoPolymorphic: Boolean = false,
        unknownChildHandler: UnknownChildHandler = DEFAULT_UNKNOWN_CHILD_HANDLER,
    ) : this(
        repairNamespaces,
        xmlDeclMode,
        indentString,
        DefaultXmlSerializationPolicy(false, autoPolymorphic, unknownChildHandler = unknownChildHandler)
    )

    @ExperimentalXmlUtilApi
    @Deprecated("Use the builder constructor that allows for ABI-safe construction with new parameters")
    public constructor(
        repairNamespaces: Boolean = true,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
        indentString: String = "",
        autoPolymorphic: Boolean? = false,
        unknownChildHandler: UnknownChildHandler? = DEFAULT_UNKNOWN_CHILD_HANDLER,
        policy: XmlSerializationPolicy,
        nilAttribute: Pair<QName, String>? = null
    ) : this(
        repairNamespaces,
        xmlDeclMode,
        indentString,
        (policy as? DefaultXmlSerializationPolicy)?.run {
            when {
                autoPolymorphic == null && unknownChildHandler == null ->
                    copy()
                autoPolymorphic == null ->
                    copy(unknownChildHandler = unknownChildHandler!!)
                unknownChildHandler == null ->
                    copy(autoPolymorphic = autoPolymorphic)
                else ->
                    copy()
            }
        } ?: policy,
        nilAttribute
    )

    /**
     * Determines whether inline classes are merged with their content. Note that inline classes
     * may still determine the tag name used for the data even if the actual contents come from
     * the child content. The actual name used is ultimately determined by the [policy] in use.
     *
     * If the value is `false` inline classes will be handled like non-inline classes
     */
    public var isInlineCollapsed: Boolean = true
        private set

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalSerializationApi::class)
    @ExperimentalXmlUtilApi
    @Deprecated("Use the primary constructor that takes a recoverable child handler")
    public constructor(
        repairNamespaces: Boolean = true,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
        indentString: String = "",
        autoPolymorphic: Boolean = false,
        unknownChildHandler: NonRecoveryUnknownChildHandler,
        policy: XmlSerializationPolicy = DefaultXmlSerializationPolicy(false, autoPolymorphic),
    ) : this(repairNamespaces, xmlDeclMode, indentString, autoPolymorphic, unknownChildHandler.asRecoverable(), policy)

    /**
     * This property determines whether the serialization will collect all used namespaces and
     * emits all namespace attributes on the root tag.
     */
    public var isCollectingNSAttributes: Boolean = false

    @ExperimentalXmlUtilApi
    @Suppress("DEPRECATION")
    @Deprecated("Use version taking XmlDeclMode")
    @OptIn(ExperimentalSerializationApi::class)
    public constructor(
        repairNamespaces: Boolean = true,
        omitXmlDecl: Boolean,
        indentString: String = "",
        autoPolymorphic: Boolean = false,
        unknownChildHandler: NonRecoveryUnknownChildHandler = DEFAULT_NONRECOVERABLE_CHILD_HANDLER
    ) : this(
        repairNamespaces,
        if (omitXmlDecl) XmlDeclMode.None else XmlDeclMode.Minimal,
        indentString,
        autoPolymorphic,
        UnknownChildHandler { input, inputKind, _, name, candidates ->
            unknownChildHandler(input, inputKind, name, candidates)
            emptyList()
        }

    )

    @ExperimentalXmlUtilApi
    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("DEPRECATION")
    @Deprecated("Use version taking XmlDeclMode")
    public constructor(
        repairNamespaces: Boolean = true,
        omitXmlDecl: Boolean,
        indent: Int,
        autoPolymorphic: Boolean = false,
        unknownChildHandler: NonRecoveryUnknownChildHandler = DEFAULT_NONRECOVERABLE_CHILD_HANDLER
    ) : this(repairNamespaces, omitXmlDecl, " ".repeat(indent), autoPolymorphic, unknownChildHandler)

    @OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
    @Suppress("DEPRECATION")
    @JvmOverloads
    public constructor(builder: Builder = Builder()) : this(
        repairNamespaces = builder.repairNamespaces,
        xmlDeclMode = builder.xmlDeclMode,
        indentString = builder.indentString,
        policy = builder.policy ?: DefaultXmlSerializationPolicy(
            pedantic = false,
            autoPolymorphic = builder.autoPolymorphic ?: false,
            encodeDefault = builder.encodeDefault,
            unknownChildHandler = builder.unknownChildHandler ?: DEFAULT_UNKNOWN_CHILD_HANDLER
        ),
        nilAttribute = builder.nilAttribute,
        xmlVersion = builder.xmlVersion
    ) {
        isInlineCollapsed = builder.isInlineCollapsed
        isCollectingNSAttributes = builder.isCollectingNSAttributes
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use indentString for better accuracy")
    public val indent: Int
        get() = indentString.countIndentedLength()

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use xmlDeclMode with more options")
    public val omitXmlDecl: Boolean
        get() = xmlDeclMode == XmlDeclMode.None

    /**
     * Configuration for the xml parser.
     *
     * @property repairNamespaces Should namespaces automatically be repaired. This option will be passed on to the [XmlWriter]
     * @property omitXmlDecl Should the generated XML contain an XML declaration or not. This is passed to the [XmlWriter]
     * @property indentString The indentation to use. This is passed to the [XmlWriter]. Note that at this point no validation
     *           of the indentation is done, if it is not valid whitespace it will produce unexpected XML.
     * @property indent The indentation level (in spaces) to use. This is derived from [indentString]. Tabs are counted as 8
     *                  characters, everything else as 1. When setting it will update [indentString] with `indent` space characters
     * @property autoPolymorphic Should polymorphic information be retrieved using [SerializersModule] configuration. This replaces
     *                     [XmlPolyChildren], but changes serialization where that annotation is not applied. This option will
     *                     become the default in the future although XmlPolyChildren will retain precedence (when present)
     * @property unknownChildHandler A function that is called when an unknown child is found. By default, an exception is thrown
     *                     but the function can silently ignore it as well.
     */
    @OptIn(ExperimentalSerializationApi::class)
    public class Builder
    @ExperimentalXmlUtilApi
    @Deprecated("This constructor has properties from the policy")
    constructor(
        public var repairNamespaces: Boolean = true,
        public var xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
        public var indentString: String = "",
        autoPolymorphic: Boolean? = null,
        unknownChildHandler: UnknownChildHandler? = DEFAULT_UNKNOWN_CHILD_HANDLER,
        @ExperimentalXmlUtilApi
        public var policy: XmlSerializationPolicy? = null
    ) {

        @ExperimentalXmlUtilApi
        @Deprecated("Use the policy instead")
        public var unknownChildHandler: UnknownChildHandler? = unknownChildHandler

        /**
         * Should polymorphic information be retrieved using [SerializersModule] configuration. This replaces
         * [XmlPolyChildren], but changes serialization where that annotation is not applied. This option will
         * become the default in the future although XmlPolyChildren will retain precedence (when present).
         *
         * This is a shortcut to the policy. If the policy has been set that value will be used.
         * Note that if the policy has been set to a default policy and this property is set
         * *afterwards*, the policy will automatically be updated.
         */
        public var autoPolymorphic: Boolean? = autoPolymorphic
            get() = field ?: (policy as? DefaultXmlSerializationPolicy)?.autoPolymorphic
            set(value) {
                field = value
                if (value != null) {
                    (policy as? DefaultXmlSerializationPolicy)?.also { p ->
                        policy = p.copy(autoPolymorphic = value)
                    }
                }
            }

        @Deprecated("This constructor has properties from the policy")
        @ExperimentalXmlUtilApi
        public constructor(
            repairNamespaces: Boolean = true,
            xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
            indentString: String = "",
            autoPolymorphic: Boolean = false,
            unknownChildHandler: NonRecoveryUnknownChildHandler,
            policy: XmlSerializationPolicy? = null
        ) : this(
            repairNamespaces,
            xmlDeclMode,
            indentString,
            autoPolymorphic,
            unknownChildHandler.asRecoverable(),
            policy
        )

        @Suppress("DEPRECATION")
        @OptIn(ExperimentalXmlUtilApi::class)
        public constructor(config: XmlConfig) : this(
            config.repairNamespaces,
            config.xmlDeclMode,
            config.indentString,
            (config.policy as? DefaultXmlSerializationPolicy)?.autoPolymorphic,
            null,
            config.policy
        ) {
            this.nilAttribute = config.nilAttribute
            this.isInlineCollapsed = config.isInlineCollapsed
            this.isCollectingNSAttributes = config.isCollectingNSAttributes
            this.xmlVersion = config.xmlVersion
        }


        @ExperimentalXmlUtilApi
        @OptIn(ExperimentalSerializationApi::class)
        @Deprecated("Use version taking XmlDeclMode")
        public constructor(
            repairNamespaces: Boolean = true,
            omitXmlDecl: Boolean,
            indentString: String = "",
            autoPolymorphic: Boolean = false,
            unknownChildHandler: NonRecoveryUnknownChildHandler = DEFAULT_NONRECOVERABLE_CHILD_HANDLER,
        ) : this(
            repairNamespaces,
            if (omitXmlDecl) XmlDeclMode.None else XmlDeclMode.Minimal,
            indentString,
            autoPolymorphic,
            unknownChildHandler.asRecoverable()
        )

        @ExperimentalXmlUtilApi
        @OptIn(ExperimentalSerializationApi::class)
        @Suppress("DEPRECATION")
        @Deprecated("Use version taking XmlDeclMode")
        public constructor(
            repairNamespaces: Boolean = true,
            omitXmlDecl: Boolean,
            indent: Int,
            autoPolymorphic: Boolean = false,
            unknownChildHandler: NonRecoveryUnknownChildHandler = DEFAULT_NONRECOVERABLE_CHILD_HANDLER,
        ) : this(repairNamespaces, omitXmlDecl, " ".repeat(indent), autoPolymorphic, unknownChildHandler)

        @ExperimentalXmlUtilApi
        @Deprecated("If using the constructor directly, use the one that uses the recoverable child handler")
        @OptIn(ExperimentalSerializationApi::class)
        public constructor(
            repairNamespaces: Boolean = true,
            xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
            indent: Int,
            autoPolymorphic: Boolean = false,
            unknownChildHandler: NonRecoveryUnknownChildHandler
        ) : this(repairNamespaces, xmlDeclMode, indent, autoPolymorphic, unknownChildHandler.asRecoverable())

        @ExperimentalXmlUtilApi
        @OptIn(ExperimentalSerializationApi::class)
        @Deprecated("This constructor has properties from the policy")
        public constructor(
            repairNamespaces: Boolean = true,
            xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
            indent: Int,
            autoPolymorphic: Boolean = false,
            unknownChildHandler: UnknownChildHandler = DEFAULT_UNKNOWN_CHILD_HANDLER
        ) : this(repairNamespaces, xmlDeclMode, " ".repeat(indent), autoPolymorphic, unknownChildHandler)

        /**
         * Determines which default values are encoded. This property gets forwarded to the policy
         */
        @Deprecated("Use the policy instead")
        public var encodeDefault: XmlEncodeDefault = XmlEncodeDefault.ANNOTATED

        /**
         * Determines whether inline classes are merged with their content. Note that inline classes
         * may still determine the tag name used for the data even if the actual contents come from
         * the child content. The actual name used is ultimately determined by the [policy] in use.
         *
         * If the value is `false` inline classes will be handled like non-inline classes
         */
        public var isInlineCollapsed: Boolean = true

        public var nilAttribute: Pair<QName, String>? = null

        public var xmlVersion: XmlVersion = XmlVersion.XML11

        /**
         * This property determines whether the serialization will collect all used namespaces and
         * emits all namespace attributes on the root tag.
         */
        public var isCollectingNSAttributes: Boolean = false

        public var indent: Int
            @Deprecated("Use indentString for better accuracy")
            get() = indentString.countIndentedLength()
            set(value) {
                indentString = " ".repeat(value)
            }

        @Deprecated("Use xmlDeclMode for this now multi-valued property")
        public var omitXmlDecl: Boolean
            get() = xmlDeclMode == XmlDeclMode.None
            set(value) {
                xmlDeclMode = when (value) {
                    true -> XmlDeclMode.None
                    else -> XmlDeclMode.Auto
                }
            }

        @OptIn(ExperimentalXmlUtilApi::class)
        public fun recommended() {
            autoPolymorphic = true
            isInlineCollapsed = true
            indent = 4
            defaultPolicy {
                pedantic = false
                typeDiscriminatorName = QName(XMLConstants.XSI_NS_URI, "type", XMLConstants.XSI_PREFIX)
                encodeDefault = XmlEncodeDefault.ANNOTATED
            }
        }

        public inline fun defaultPolicy(configure: DefaultXmlSerializationPolicy.Builder.() -> Unit) {
            policy = policyBuilder().apply(configure).build()
        }

        @PublishedApi
        internal fun policyBuilder(): DefaultXmlSerializationPolicy.Builder = when (val p = policy){
            is DefaultXmlSerializationPolicy -> DefaultXmlSerializationPolicy.Builder(p)
            else -> DefaultXmlSerializationPolicy.Builder()
        }
    }

    public companion object {
        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        @OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
        public val DEFAULT_UNKNOWN_CHILD_HANDLER: UnknownChildHandler =
            UnknownChildHandler { input, inputKind, descriptor, name, candidates ->
                if (inputKind == InputKind.Attribute && name?.namespaceURI==XMLConstants.XSI_NS_URI) {
                    emptyList()
                } else {
                    throw UnknownXmlFieldException(input.locationInfo, "(${descriptor.serialDescriptor.serialName}) ${descriptor.tagName}/${name ?: "<CDATA>"} ($inputKind)", candidates)
                }
            }

        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        @OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
        public val DEFAULT_NONRECOVERABLE_CHILD_HANDLER: NonRecoveryUnknownChildHandler =
            { input, inputKind, name, candidates ->
                throw UnknownXmlFieldException(input.locationInfo, name?.toString() ?: "<CDATA>", candidates)
            }
    }
}

@ExperimentalXmlUtilApi
internal inline fun NonRecoveryUnknownChildHandler.asRecoverable(): UnknownChildHandler {
    return UnknownChildHandler { input, inputKind, _, name, candidates ->
        this(input, inputKind, name, candidates)
        emptyList()
    }
}

@ExperimentalXmlUtilApi
public typealias NonRecoveryUnknownChildHandler = (input: XmlReader, inputKind: InputKind, name: QName?, candidates: Collection<Any>) -> Unit

@ExperimentalXmlUtilApi
public fun interface UnknownChildHandler {

    public fun handleUnknownChildRecovering(
        input: XmlReader,
        inputKind: InputKind,
        descriptor: XmlDescriptor,
        name: QName?,
        candidates: Collection<Any>
    ): List<XML.ParsedData<*>>
}

