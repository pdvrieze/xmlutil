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

@file:OptIn(XmlUtilInternal::class)

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.internal.countLength

/**
 * Configuration for the xml parser.
 *
 * @property repairNamespaces Should namespaces automatically be repaired. This option will be passed on to the [XmlWriter]
 * @property omitXmlDecl Should the generated XML contain an XML declaration or not. This is passed to the [XmlWriter]
 * @property indentString The indentation to use. This is passed to the [XmlWriter]. Note that at this point no validation
 *           of the indentation is done, if it is not valid whitespace it will produce unexpected XML.
 * @property indent The indentation level (in spaces) to use. This is derived from [indentString]. Tabs are counted as 8
 *                  characters, everything else as 1
 * @property autoPolymorphic Should polymorphic information be retrieved using [SerializersModule] configuration. This replaces
 *                     [XmlPolyChildren], but changes serialization where that annotation is not applied. This option will
 *                     become the default in the future although XmlPolyChildren will retain precedence (when present)
 * @property unknownChildHandler A function that is called when an unknown child is found. By default an exception is thrown
 *                     but the function can silently ignore it as well.
 */
class XmlConfig
@OptIn(ExperimentalSerializationApi::class)
constructor(
    val repairNamespaces: Boolean = true,
    val xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
    val indentString: String = "",
    autoPolymorphic: Boolean = false,
    unknownChildHandler: UnknownChildHandler = DEFAULT_UNKNOWN_CHILD_HANDLER,
    val policy: XmlSerializationPolicy = DefaultXmlSerializationPolicy(false, autoPolymorphic)
                                                                       ) {

    var isInlineCollapsed: Boolean = true
        private set

    @Deprecated("Use version taking XmlDeclMode")
    constructor(
            repairNamespaces: Boolean = true,
            omitXmlDecl: Boolean,
            indentString: String = "",
            autoPolymorphic: Boolean = false,
            unknownChildHandler: UnknownChildHandler = DEFAULT_UNKNOWN_CHILD_HANDLER
               ): this(
        repairNamespaces,
        if(omitXmlDecl) XmlDeclMode.None else XmlDeclMode.Minimal,
        indentString,
        autoPolymorphic,
        unknownChildHandler
                      )

    @Suppress("DEPRECATION")
    @Deprecated("Use version taking XmlDeclMode")
    constructor(
        repairNamespaces: Boolean = true,
        omitXmlDecl: Boolean,
        indent: Int,
        autoPolymorphic: Boolean = false,
        unknownChildHandler: UnknownChildHandler = DEFAULT_UNKNOWN_CHILD_HANDLER
               ) : this(repairNamespaces, omitXmlDecl, " ".repeat(indent), autoPolymorphic, unknownChildHandler)

    constructor(builder: Builder) : this(
        builder.repairNamespaces,
        builder.xmlDeclMode,
        builder.indentString,
        builder.autoPolymorphic,
        builder.unknownChildHandler,
        builder.policy?: DefaultXmlSerializationPolicy(false, builder.autoPolymorphic)
                                        ) {
        isInlineCollapsed = builder.isInlineCollapsed
    }

    @OptIn(ExperimentalSerializationApi::class)
    val unknownChildHandler: UnknownChildHandler = when (unknownChildHandler) {
        DEFAULT_UNKNOWN_CHILD_HANDLER -> { input, inputKind, name, candidates ->
            policy.handleUnknownContent(input, inputKind, name, candidates)
        }
        else -> unknownChildHandler
    }

    @Deprecated("Use indentString for better accuracy")
    val indent: Int
        get() = indentString.countLength()

    @Deprecated("Use xmlDeclMode with more options")
    val omitXmlDecl: Boolean
        get() = xmlDeclMode== XmlDeclMode.None

    /**
     * Configuration for the xml parser.
     *
     * @property repairNamespaces Should namespaces automatically be repaired. This option will be passed on to the [XmlWriter]
     * @property omitXmlDecl Should the generated XML contain an XML declaration or not. This is passed to the [XmlWriter]
     * @property indentString The indentation to use. This is passed to the [XmlWriter]. Note that at this point no validation
     *           of the indentation is done, if it is not valid whitespace it will produce unexpected XML.
     * @property indent The indentation level (in spaces) to use. This is derived from [indentString]. Tabs are counted as 8
     *                  characters, everything else as 1. When setting it it will update [indentString] with `indent` space characters
     * @property autoPolymorphic Should polymorphic information be retrieved using [SerializersModule] configuration. This replaces
     *                     [XmlPolyChildren], but changes serialization where that annotation is not applied. This option will
     *                     become the default in the future although XmlPolyChildren will retain precedence (when present)
     * @property unknownChildHandler A function that is called when an unknown child is found. By default an exception is thrown
     *                     but the function can silently ignore it as well.
     */
    class Builder constructor(
        var repairNamespaces: Boolean = true,
        var xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
        var indentString: String = "",
        var autoPolymorphic: Boolean = false,
        var unknownChildHandler: UnknownChildHandler = DEFAULT_UNKNOWN_CHILD_HANDLER,
        @OptIn(ExperimentalSerializationApi::class)
        var policy: XmlSerializationPolicy? = null
                                                                         ) {

        @Deprecated("Use version taking XmlDeclMode")
        constructor(
            repairNamespaces: Boolean = true,
            omitXmlDecl: Boolean,
            indentString: String = "",
            autoPolymorphic: Boolean = false,
            unknownChildHandler: UnknownChildHandler = DEFAULT_UNKNOWN_CHILD_HANDLER
                   ): this(
            repairNamespaces,
            if(omitXmlDecl) XmlDeclMode.None else XmlDeclMode.Minimal,
            indentString,
            autoPolymorphic,
            unknownChildHandler
                          )

        @Suppress("DEPRECATION")
        @Deprecated("Use version taking XmlDeclMode")
        constructor(
            repairNamespaces: Boolean = true,
            omitXmlDecl: Boolean,
            indent: Int,
            autoPolymorphic: Boolean = false,
            unknownChildHandler: UnknownChildHandler = DEFAULT_UNKNOWN_CHILD_HANDLER
                   ) : this(repairNamespaces, omitXmlDecl, " ".repeat(indent), autoPolymorphic, unknownChildHandler)

        constructor(
            repairNamespaces: Boolean = true,
            xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
            indent: Int,
            autoPolymorphic: Boolean = false,
            unknownChildHandler: UnknownChildHandler = DEFAULT_UNKNOWN_CHILD_HANDLER
                   ) : this(repairNamespaces, xmlDeclMode, " ".repeat(indent), autoPolymorphic, unknownChildHandler)

        var isInlineCollapsed: Boolean = true

        var indent: Int
            @Deprecated("Use indentString for better accuracy")
            get() = indentString.countLength()
            set(value) { indentString = " ".repeat(value) }

        @Deprecated("Use xmlDeclMode for this now multi-valued property")
        var omitXmlDecl
            get() = xmlDeclMode == XmlDeclMode.None
            set(value) { xmlDeclMode = when(value) {
                true -> XmlDeclMode.None
                else -> XmlDeclMode.Auto
            }}
    }

    companion object {
        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        val DEFAULT_UNKNOWN_CHILD_HANDLER: UnknownChildHandler =
            { input, inputKind, name, candidates ->
                throw UnknownXmlFieldException(
                    input.locationInfo,
                    name?.toString()?:"<CDATA>",
                    candidates
                                                                                                     )
            }
    }
}

typealias UnknownChildHandler = (input: XmlReader, inputKind: InputKind, name: QName?, candidates: Collection<Any>) -> Unit
