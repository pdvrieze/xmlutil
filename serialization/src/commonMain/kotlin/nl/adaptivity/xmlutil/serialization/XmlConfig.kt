/*
 * Copyright (c) 2024-2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.core.internal.countIndentedLength
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.XmlEncodeDefault
import nl.adaptivity.xmlutil.serialization.impl.ShadowPolicy
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlTypeDescriptor
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
private constructor(
    public val repairNamespaces: Boolean = true,
    public val xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
    public val indentString: String = "",
    policy: XmlSerializationPolicy,
    nilAttribute: Pair<QName, String>? = null,
    public val xmlVersion: XmlVersion = XmlVersion.XML11,
    cachingEnabled: Boolean = true,
) {

    /**
     * Determines whether inline classes are merged with their content. Note that inline classes
     * may still determine the tag name used for the data even if the actual contents come from
     * the child content. The actual name used is ultimately determined by the [policy] in use.
     *
     * If the value is `false` inline classes will be handled like non-inline classes
     */
    public var isInlineCollapsed: Boolean = true
        private set

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use indentString for better accuracy")
    public val indent: Int
        get() = indentString.countIndentedLength()

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use xmlDeclMode with more options")
    public val omitXmlDecl: Boolean
        get() = xmlDeclMode == XmlDeclMode.None

    public val policy: XmlSerializationPolicy = when (policy) {
        is ShadowPolicy -> policy.basePolicy
        else -> policy
    }

    public val formatCache: FormatCache = when (policy) {
        is DefaultXmlSerializationPolicy -> policy.formatCache
        is ShadowPolicy -> policy.cache
        else -> if (cachingEnabled) defaultSharedFormatCache() else FormatCache.Dummy
    }

    public val nilAttributeName: QName? = nilAttribute?.first
    public val nilAttributeValue: String? = nilAttribute?.second

    public val nilAttribute: Pair<QName, String>?
        get() = nilAttributeName?.run {
            Pair(nilAttributeName, nilAttributeValue!!)
        }

    /**
     * This property determines whether the serialization will collect all used namespaces and
     * emits all namespace attributes on the root tag.
     */
    public var isCollectingNSAttributes: Boolean = false
        private set

    /**
     * This property can be used to disable various checks on the correctness of the serializer descriptions.
     * This should speed up processing, but may give surprising results in the presence of an error. Note that
     * this doesn't disable all checks, but mainly expensive ones on matters like order, or checks on
     * serial format. This does not disable the checking
     */
    public var isUnchecked: Boolean = false
        private set

    public var isAlwaysDecodeXsiNil: Boolean = true
        private set

    public var defaultToGenericParser: Boolean = false
        private set

    @Suppress("DEPRECATION")
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
                    copy { }

                autoPolymorphic == null ->
                    copy { this.unknownChildHandler = unknownChildHandler!! }

                unknownChildHandler == null ->
                    copy { this.autoPolymorphic = autoPolymorphic }

                else ->
                    copy { }
            }
        } ?: policy,
        nilAttribute
    )

    @Suppress("DEPRECATION")
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

    @ExperimentalXmlUtilApi
    @Suppress("DEPRECATION")
    @Deprecated("Use version taking XmlDeclMode")
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
    @Suppress("DEPRECATION")
    @Deprecated("Use version taking XmlDeclMode")
    public constructor(
        repairNamespaces: Boolean = true,
        omitXmlDecl: Boolean,
        indent: Int,
        autoPolymorphic: Boolean = false,
        unknownChildHandler: NonRecoveryUnknownChildHandler = DEFAULT_NONRECOVERABLE_CHILD_HANDLER
    ) : this(repairNamespaces, omitXmlDecl, " ".repeat(indent), autoPolymorphic, unknownChildHandler)

    @OptIn(ExperimentalXmlUtilApi::class)
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
        xmlVersion = builder.xmlVersion,
        cachingEnabled = builder.isCachingEnabled,
    ) {
        isInlineCollapsed = builder.isInlineCollapsed
        isCollectingNSAttributes = builder.isCollectingNSAttributes
        isUnchecked = builder.isUnchecked
        isAlwaysDecodeXsiNil = builder.isAlwaysDecodeXsiNil
        defaultToGenericParser = builder.defaultToGenericParser
    }

    internal fun lookupTypeDesc(parentNamespace: Namespace, serialDescriptor: SerialDescriptor): XmlTypeDescriptor {
        return formatCache.lookupTypeOrStore(parentNamespace, serialDescriptor) {
            XmlTypeDescriptor(this, serialDescriptor, parentNamespace)
        }
    }

    internal fun shadowCache(cache: FormatCache): XmlConfig {
        return XmlConfig(Builder(this).apply {
            policy = ShadowPolicy(this@XmlConfig.policy, cache)
        })
    }

    @Suppress("DuplicatedCode")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XmlConfig

        if (repairNamespaces != other.repairNamespaces) return false
        if (xmlDeclMode != other.xmlDeclMode) return false
        if (indentString != other.indentString) return false
        if (policy != other.policy) return false
        if (xmlVersion != other.xmlVersion) return false
        if (isInlineCollapsed != other.isInlineCollapsed) return false
        if (nilAttributeName != other.nilAttributeName) return false
        if (nilAttributeValue != other.nilAttributeValue) return false
        if (isCollectingNSAttributes != other.isCollectingNSAttributes) return false
        if (isUnchecked != other.isUnchecked) return false
        if (isAlwaysDecodeXsiNil != other.isAlwaysDecodeXsiNil) return false
        if (defaultToGenericParser != other.defaultToGenericParser) return false

        return true
    }

    @Suppress("DuplicatedCode")
    override fun hashCode(): Int {
        var result = repairNamespaces.hashCode()
        result = 31 * result + xmlDeclMode.hashCode()
        result = 31 * result + indentString.hashCode()
        result = 31 * result + policy.hashCode()
        result = 31 * result + xmlVersion.hashCode()
        result = 31 * result + isInlineCollapsed.hashCode()
        result = 31 * result + (nilAttributeName?.hashCode() ?: 0)
        result = 31 * result + (nilAttributeValue?.hashCode() ?: 0)
        result = 31 * result + isCollectingNSAttributes.hashCode()
        result = 31 * result + isUnchecked.hashCode()
        result = 31 * result + isAlwaysDecodeXsiNil.hashCode()
        result = 31 * result + defaultToGenericParser.hashCode()
        return result
    }


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
    public class Builder
    @ExperimentalXmlUtilApi
    @Deprecated("This constructor has properties from the policy")
    constructor(
        public var repairNamespaces: Boolean = true,
        public var xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
        public var indentString: String = "",
        autoPolymorphic: Boolean? = null,
        @ExperimentalXmlUtilApi
        @Deprecated("Use the policy instead")
        public var unknownChildHandler: UnknownChildHandler? = DEFAULT_UNKNOWN_CHILD_HANDLER,
        policy: XmlSerializationPolicy? = null
    ) {

        @ExperimentalXmlUtilApi
        public var policy: XmlSerializationPolicy? = policy
            set(value) {
                field = value
                if (autoPolymorphic != null) {
                    if (value is DefaultXmlSerializationPolicy && value.autoPolymorphic != autoPolymorphic) {
                        this.autoPolymorphic = value.autoPolymorphic
                    }
                }
                if (value is DefaultXmlSerializationPolicy && value.formatCache == FormatCache.Dummy && isCachingEnabled) {
                    isCachingEnabled = false
                }
            }

        /**
         * Should polymorphic information be retrieved using [SerializersModule] configuration. This replaces
         * [XmlPolyChildren], but changes serialization where that annotation is not applied. This option will
         * become the default in the future although XmlPolyChildren will retain precedence (when present).
         *
         * This is a shortcut to the policy. If the policy has been set that value will be used.
         * Note that if the policy has been set to a default policy and this property is set
         * *afterwards*, the policy will automatically be updated.
         */
        @OptIn(ExperimentalXmlUtilApi::class)
        public var autoPolymorphic: Boolean? = autoPolymorphic
            get() = field ?: (policy as? DefaultXmlSerializationPolicy)?.autoPolymorphic
            set(value) {
                field = value
                if (value != null) {
                    when (val p = policy) {
                        null -> defaultPolicy { autoPolymorphic = value }
                        is DefaultXmlSerializationPolicy -> if (p.autoPolymorphic != value) {
                            policy = p.copy { autoPolymorphic = value }
                        }
                    }
                }
            }

        @Suppress("DEPRECATION")
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
            repairNamespaces = config.repairNamespaces,
            xmlDeclMode = config.xmlDeclMode,
            indentString = config.indentString,
            autoPolymorphic = (config.policy as? DefaultXmlSerializationPolicy)?.autoPolymorphic,
            unknownChildHandler = (config.policy as? DefaultXmlSerializationPolicy)?.unknownChildHandler,
            policy = config.policy
        ) {
            this.nilAttribute = config.nilAttribute
            this.isInlineCollapsed = config.isInlineCollapsed
            this.isCollectingNSAttributes = config.isCollectingNSAttributes
            this.xmlVersion = config.xmlVersion
            this.isUnchecked = config.isUnchecked
            this.isAlwaysDecodeXsiNil = config.isAlwaysDecodeXsiNil
            this.defaultToGenericParser = config.defaultToGenericParser
        }

        @Suppress("DEPRECATION")
        @ExperimentalXmlUtilApi
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
        @Suppress("DEPRECATION")
        @Deprecated("Use version taking XmlDeclMode")
        public constructor(
            repairNamespaces: Boolean = true,
            omitXmlDecl: Boolean,
            indent: Int,
            autoPolymorphic: Boolean = false,
            unknownChildHandler: NonRecoveryUnknownChildHandler = DEFAULT_NONRECOVERABLE_CHILD_HANDLER,
        ) : this(repairNamespaces, omitXmlDecl, " ".repeat(indent), autoPolymorphic, unknownChildHandler)

        @Suppress("DEPRECATION")
        @ExperimentalXmlUtilApi
        @Deprecated("If using the constructor directly, use the one that uses the recoverable child handler")
        public constructor(
            repairNamespaces: Boolean = true,
            xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
            indent: Int,
            autoPolymorphic: Boolean = false,
            unknownChildHandler: NonRecoveryUnknownChildHandler
        ) : this(repairNamespaces, xmlDeclMode, indent, autoPolymorphic, unknownChildHandler.asRecoverable())

        @Suppress("DEPRECATION")
        @ExperimentalXmlUtilApi
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

        /**
         * Configuration that specifies whether an attribute is used to indicate a null value (such
         * as the xsi:nil attribute from xml schema). The value is a pair of the QName of the attribute
         * and the string to indicate that the value is in fact null.
         *
         * For serializing this property means that (for tags only ) the tag will be written with empty
         * content and the given attribute. For attributes or a `null` value the element is not written
         * at all.
         *
         * *Note* that the `isAlwaysDecodeXsiNil` attribute will allow decoding to decode the xsi:nil
         * attribute even if no nil attribute is configured.
         */
        public var nilAttribute: Pair<QName, String>? = null

        public var xmlVersion: XmlVersion = XmlVersion.XML11

        public var isCachingEnabled: Boolean = true
            set(value) {
                if (field != value) {
                    field = value

                    // If this is changed, also update the policy
                    val p = policy
                    if (p is DefaultXmlSerializationPolicy) {
                        policy = p.builder().apply { isCachingEnabled = value }.build()
                    }
                }
            }

        /**
         * This property determines whether the serialization will collect all used namespaces and
         * emits all namespace attributes on the root tag.
         */
        public var isCollectingNSAttributes: Boolean = false

        /**
         * Default parsing to the optimized generic parser rather than using the platform specific one
         */
        public var defaultToGenericParser: Boolean = false

        /**
         * This property can be used to disable various checks on the correctness of the serializer descriptions.
         * This should speed up processing, but may give surprising results in the presence of an error.
         */
        public var isUnchecked: Boolean = false

        /**
         * Allow decoding `xsi:nil` (from the xml schema instance namespace) independent of the configured
         * nil attribute (that will also be detected if present). This does not change engoding/serialization.
         */
        public var isAlwaysDecodeXsiNil: Boolean = true

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

        /**
         * Configure the parser using the latest recommended settings. Note that this function has
         * no guarantee of stability.
         */
        public fun recommended() {
            recommended { }
        }

        /**
         * Configure the policy and the config builder with the latest recommended settings and policy.
         * Note that this function has no guarantee of stability.
         */
        public inline fun recommended(configurePolicy: DefaultXmlSerializationPolicy.Builder.() -> Unit) {
            recommended_0_91_0(configurePolicy)
        }

        /**
         * Configure the parser using the latest recommended settings for fast (en/de)coding. Note that this function has
         * no guarantee of stability.
         */
        public fun fast() {
            fast { }
        }

        /**
         * Configure the policy and the config builder with the latest recommended settings and policy.
         * Note that this function has no guarantee of stability.
         */
        public inline fun fast(configurePolicy: DefaultXmlSerializationPolicy.Builder.() -> Unit) {
            fast_0_91_1(configurePolicy)
        }

        /**
         * Configure the format using the recommended configuration as of version 0.86.3. This configuration is stable.
         */
        @Suppress("FunctionName", "DEPRECATION")
        @Deprecated("Consider updating to a newer recommended configuration", ReplaceWith("recommended_0_91_0()"))
        public fun recommended_0_86_3() {
            recommended_0_86_3 { }
        }

        /**
         * Configure the format starting with the recommended configuration as of version 0.86.3. This configuration is stable.
         */
        @Suppress("FunctionName")
        @Deprecated("Consider updating to a newer recommended configuration", ReplaceWith("recommended_0_91_0(configurePolicy)"))
        public inline fun recommended_0_86_3(configurePolicy: DefaultXmlSerializationPolicy.Builder.() -> Unit) {
            autoPolymorphic = true
            isInlineCollapsed = true
            indent = 4
            defaultPolicy {
                autoPolymorphic = true
                pedantic = false
                typeDiscriminatorName = QName(XMLConstants.XSI_NS_URI, "type", XMLConstants.XSI_PREFIX)
                encodeDefault = XmlEncodeDefault.ANNOTATED
                throwOnRepeatedElement = true
                isStrictAttributeNames = true
                configurePolicy()
            }
        }

        /**
         * Configure the format using the recommended configuration as of version 0.87.0. This configuration is stable.
         */
        @Suppress("FunctionName")
        public fun recommended_0_87_0() {
            val hadAutoPolymorphic = autoPolymorphic != null
            recommended_0_87_0 { }
            if (!hadAutoPolymorphic) autoPolymorphic = null
        }

        /**
         * Configure the format starting with the recommended configuration as of version 0.87.0. This configuration is stable.
         */
        @Suppress("FunctionName", "DEPRECATION")
        public inline fun recommended_0_87_0(configurePolicy: DefaultXmlSerializationPolicy.Builder.() -> Unit) {
            repairNamespaces = false
            recommended_0_86_3 {
                isStrictOtherAttributes = true
                configurePolicy()
            }
        }

        /**
         * Configure the format using the recommended configuration as of version 0.87.0. This configuration is stable.
         */
        @Suppress("FunctionName")
        public fun recommended_0_90_2() {
            val hadAutoPolymorphic = autoPolymorphic != null
            recommended_0_90_2 { }
            if (!hadAutoPolymorphic) autoPolymorphic = null
        }

        /**
         * Configure the format starting with the recommended configuration as of version 0.87.0. This configuration is stable.
         * Note that this defaults to xml 1.1 with (minimal) document type declaration. A document type declaration is
         * required for XML 1.1 (otherwise it reverts to 1.0).
         */
        @Suppress("FunctionName")
        public inline fun recommended_0_90_2(configurePolicy: DefaultXmlSerializationPolicy.Builder.() -> Unit) {
            repairNamespaces = false
            recommended_0_87_0 {
                xmlVersion = XmlVersion.XML11
                xmlDeclMode = XmlDeclMode.Minimal
                isStrictBoolean = true
                configurePolicy()
            }
        }

        /**
         * Configure the format using the recommended configuration as of version 0.87.0. This configuration is stable.
         */
        @Suppress("FunctionName")
        public fun recommended_0_91_0() {
            val hadAutoPolymorphic = autoPolymorphic != null
            recommended_0_91_0 { }
            if (!hadAutoPolymorphic) autoPolymorphic = null
        }

        /**
         * Configure the format starting with the recommended configuration as of version 0.87.0. This configuration is stable.
         * Note that this defaults to xml 1.1 with (minimal) document type declaration. A document type declaration is
         * required for XML 1.1 (otherwise it reverts to 1.0).
         */
        @Suppress("FunctionName")
        public inline fun recommended_0_91_0(configurePolicy: DefaultXmlSerializationPolicy.Builder.() -> Unit) {
            recommended_0_90_2 {
                isXmlFloat = true
                configurePolicy()
            }
        }

        /**
         * Configure the format using the recommended configuration as of version 0.87.0. This configuration is stable.
         */
        @Suppress("FunctionName")
        public fun fast_0_90_2() {
            val hadAutoPolymorphic = autoPolymorphic != null
            fast_0_90_2 { }
            if (!hadAutoPolymorphic) autoPolymorphic = null
        }

        /**
         * Configure the format starting with the recommended configuration as of version 0.87.0. This configuration is stable.
         * Note that this defaults to xml 1.1 with (minimal) document type declaration. A document type declaration is
         * required for XML 1.1 (otherwise it reverts to 1.0).
         */
        @Suppress("FunctionName")
        public inline fun fast_0_90_2(configurePolicy: DefaultXmlSerializationPolicy.Builder.() -> Unit) {
            isCachingEnabled = true
            recommended_0_90_2 {
                isAlwaysDecodeXsiNil = false
                isUnchecked = true
                isCollectingNSAttributes = false
                defaultToGenericParser = true
                indentString = ""
                configurePolicy()
            }
        }

        /**
         * Configure the format using the recommended configuration as of version 0.87.0. This configuration is stable.
         */
        @Suppress("FunctionName")
        public fun fast_0_91_1() {
            val hadAutoPolymorphic = autoPolymorphic != null
            fast_0_91_1 { }
            if (!hadAutoPolymorphic) autoPolymorphic = null
        }

        /**
         * Configure the format starting with the recommended configuration as of version 0.87.0. This configuration is stable.
         * Note that this defaults to xml 1.1 with (minimal) document type declaration. A document type declaration is
         * required for XML 1.1 (otherwise it reverts to 1.0).
         */
        @Suppress("FunctionName")
        public inline fun fast_0_91_1(configurePolicy: DefaultXmlSerializationPolicy.Builder.() -> Unit) {
            isCachingEnabled = true
            recommended_0_91_0 {
                isAlwaysDecodeXsiNil = false
                isUnchecked = true
                isCollectingNSAttributes = false
                defaultToGenericParser = true
                indentString = ""

                configurePolicy()
            }
        }

        /**
         * This function allows configuring the policy based on the default (or already set
         * configuration). When the policy set is (derived from) `DefaultXmlSerializationPolicy`,
         * this is effectively equivalent to getting access to its builder to update the settings.
         *
         * If the already set policy does not inherit `DefaultXmlSerializationPolicy` this will
         * set the policy to a new default policy with default configuration (inheriting only the
         * properties defined on `XmlSerializationPolicy`).
         */
        @OptIn(ExperimentalXmlUtilApi::class)
        public inline fun defaultPolicy(configure: DefaultXmlSerializationPolicy.Builder.() -> Unit) {
            val defaultPolicy = policyBuilder().apply(configure).build()
            policy = defaultPolicy
            if (autoPolymorphic != null) autoPolymorphic = defaultPolicy.autoPolymorphic
        }

        @OptIn(ExperimentalXmlUtilApi::class)
        @PublishedApi
        internal fun policyBuilder(): DefaultXmlSerializationPolicy.Builder = when (val p = policy) {
            is DefaultXmlSerializationPolicy -> p.builder()
            null -> DefaultXmlSerializationPolicy.Builder()
            else -> DefaultXmlSerializationPolicy.Builder(p)
        }.also { autoPolymorphic?.let { ap -> it.autoPolymorphic = ap } }
    }

    public companion object {
        private val DEFAULT_IGNORED_NAMESPACES = arrayOf(XMLConstants.XSI_NS_URI, XMLConstants.XML_NS_URI)

        @OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
        public val DEFAULT_UNKNOWN_CHILD_HANDLER: UnknownChildHandler =
            UnknownChildHandler { input, inputKind, descriptor, name, candidates ->
                if (inputKind == InputKind.Attribute && name?.namespaceURI in DEFAULT_IGNORED_NAMESPACES) {
                    emptyList()
                } else {
                    throw UnknownXmlFieldException(
                        input.extLocationInfo,
                        "(${descriptor.serialDescriptor.serialName}) ${descriptor.tagName}/${name ?: "<CDATA>"} ($inputKind)",
                        candidates
                    )
                }
            }

        @OptIn(ExperimentalXmlUtilApi::class)
        public val IGNORING_UNKNOWN_CHILD_HANDLER: UnknownChildHandler =
            UnknownChildHandler { _, _, _, _, _ ->
                emptyList()
            }

        @OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
        public val IGNORING_UNKNOWN_NAMESPACE_HANDLER: UnknownChildHandler =
            UnknownChildHandler { input, inputKind, descriptor, name, candidates ->
                val inputNs = input.namespaceURI
                val contextNs = descriptor.tagName.namespaceURI

                if (inputNs != contextNs || (inputKind == InputKind.Attribute && name?.namespaceURI == XMLConstants.XSI_NS_URI)) {
                    emptyList()
                } else {
                    throw UnknownXmlFieldException(
                        input.extLocationInfo,
                        "(${descriptor.serialDescriptor.serialName}) ${descriptor.tagName}/${name ?: "<CDATA>"} ($inputKind)",
                        candidates
                    )
                }
            }

        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        @OptIn(ExperimentalXmlUtilApi::class)
        @Deprecated("Use UnknownChildHander instead that supports recovering from unknown children")
        public val DEFAULT_NONRECOVERABLE_CHILD_HANDLER: NonRecoveryUnknownChildHandler =
            { input, inputKind, name, candidates ->
                throw UnknownXmlFieldException(input.extLocationInfo, name?.toString() ?: "<CDATA>", candidates)
            }
    }
}

@ExperimentalXmlUtilApi
internal fun NonRecoveryUnknownChildHandler.asRecoverable(): UnknownChildHandler {
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

