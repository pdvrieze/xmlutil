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
@file:OptIn(ExperimentalSerializationApi::class)

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Specify more detailed name information than can be provided by [kotlinx.serialization.SerialName].
 * @property value The local part of the name
 * @property namespace The namespace to use
 * @property prefix the Prefix to use
 */
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class XmlSerialName(
    val value: String,
    val namespace: String/* = UNSET_ANNOTATION_VALUE*/,
    val prefix: String/* = UNSET_ANNOTATION_VALUE*/
                              )

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
annotation class XmlChildrenName(
    val value: String,
    val namespace: String/* = UNSET_ANNOTATION_VALUE*/,
    val prefix: String/* = UNSET_ANNOTATION_VALUE*/
                                )

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
 * Require this property to be serialized before other (sibling) properties.
 * The names are the serialNames of the properties being serialized (not
 * XML names). Together [XmlBefore] and [XmlAfter] define a partial order
 * over the properties.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlBefore(vararg val value: String)

/**
 * Require this property to be serialized after other (sibling) properties.
 * The names are the serialNames of the properties being serialized (not
 * XML names).
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XmlAfter(vararg val value: String)

/**
 * Default value for unset annotations
 */
internal const val UNSET_ANNOTATION_VALUE = "ZXC\u0001VBNBVCXZ"
