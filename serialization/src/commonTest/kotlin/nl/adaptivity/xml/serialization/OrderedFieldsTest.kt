/*
 * Copyright (c) 2021.
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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlAfter
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class OrderedFieldsTest : TestBase<OrderedFieldsTest.Employee>(
    Employee("John", "Doe", "1980-02-29", "New York", 12345, "Principal Engineer", 666),
    Employee.serializer()
                                                              ) {
    override val expectedXML: String =
        "<Employee lineManagerId=\"666\" employeeStatus=\"active\">" +
                "<birthPlace>New York</birthPlace>" +
                "<employeeNumber>12345</employeeNumber>" +
                "<familyName>Doe</familyName>" +
                "<givenName>John</givenName>" +
                "<jobTitle>Principal Engineer</jobTitle>" +
                "<birthDate>1980-02-29</birthDate>" +
                "</Employee>"
    override val expectedJson: String =
        "{\"givenName\":\"John\",\"familyName\":\"Doe\",\"birthDate\":\"1980-02-29\",\"birthPlace\":\"New York\",\"employeeNumber\":12345,\"jobTitle\":\"Principal Engineer\",\"employeeStatus\":\"active\",\"lineManagerId\":666}"

    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Serializable
    open class Person constructor(
        @XmlElement(true)
        val givenName: String,
        @XmlElement(true)
        @XmlBefore("givenName")
        val familyName: String,
        @XmlElement(true)
        @XmlAfter("familyName", "givenName")
        val birthDate: String,
        @XmlElement(true)
        val birthPlace: String
                                 ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Person

            if (givenName != other.givenName) return false
            if (familyName != other.familyName) return false
            if (birthDate != other.birthDate) return false
            if (birthPlace != other.birthPlace) return false

            return true
        }

        override fun hashCode(): Int {
            var result = givenName.hashCode()
            result = 31 * result + familyName.hashCode()
            result = 31 * result + birthDate.hashCode()
            result = 31 * result + birthPlace.hashCode()
            return result
        }
    }


    @Serializable
    class Employee : Person {
        @XmlElement(true)
        @XmlBefore("familyName")
        val employeeNumber: Int
        @XmlElement(true)
        @XmlBefore("birthDate")
        @XmlAfter("employeeNumber")
        val jobTitle: String

        @XmlElement(false)
        val employeeStatus: String
        @XmlBefore("employeeStatus")
        @XmlElement(false)
        val lineManagerId: Int

        constructor(
            givenName: String,
            familyName: String,
            birthDate: String,
            birthPlace: String,
            employeeNumber: Int,
            jobTitle: String,
            lineManagerId: Int,
            status: String = "active"
                   ) : super(givenName, familyName, birthDate, birthPlace) {
            this.employeeNumber = employeeNumber
            this.jobTitle = jobTitle
            this.employeeStatus = status
            this.lineManagerId = lineManagerId
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            if (!super.equals(other)) return false

            other as Employee

            if (employeeNumber != other.employeeNumber) return false
            if (jobTitle != other.jobTitle) return false
            if (employeeStatus != other.employeeStatus) return false
            if (lineManagerId != other.lineManagerId) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + employeeNumber
            result = 31 * result + jobTitle.hashCode()
            result = 31 * result + employeeStatus.hashCode()
            result = 31 * result + lineManagerId
            return result
        }


    }

}