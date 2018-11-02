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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.SerialInfo

/**
 * Specify more detailed name information than can be provided by [SerialName].
 * @property value The local part of the name
 * @property namespace The namespace to use
 * @property prefix the Prefix to use
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class XmlSerialName(val value: String,
                               val namespace: String/* = UNSET_ANNOTATION_VALUE*/,
                               val prefix: String/* = UNSET_ANNOTATION_VALUE*/)

/**
 * Indicate the valid poly children for this element
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlPolyChildren(val value: Array<String>)

/**
 * Specify additional information about child values. This is only used for primitives, not for classes that have their
 * own independent name
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class XmlChildrenName(val value: String,
                                 val namespace: String/* = UNSET_ANNOTATION_VALUE*/,
                                 val prefix: String/* = UNSET_ANNOTATION_VALUE*/)

/**
 * Force a property that could be an attribute to be an element
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class XmlElement(val value: Boolean/* = true*/)

/**
 * Force a property to be text element content
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlValue(val value: Boolean /*= true*/)

/**
 * Allow a property to be omitted with a default serialized string
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlDefault(val value: String)

/**
 * Default value for unset annotations
 */
internal const val UNSET_ANNOTATION_VALUE = "ZXCVBNBVCXZ"
