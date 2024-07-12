import net.devrieze.gradle.ext.addNativeTargets

/*
 * Copyright (c) 2024.
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

plugins {
    alias(libs.plugins.benchmark)
    kotlin("multiplatform")
    id("projectPlugin")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.allopen)
    alias(libs.plugins.dokka)
//    alias(libs.plugins.jmh)
    signing
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.core)
                implementation(projects.xmlschema)
                implementation(projects.schemaTests)
                implementation(projects.serialization)
                implementation(projects.testutil)
                implementation(libs.benchmark.runtime)
                implementation(libs.datetime)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(projects.coreJdk)
//                implementation(libs.jmhCore)
                implementation(kotlin("test-junit5"))
            }
        }
        val jvmTest by getting {
            dependencies {
                runtimeOnly(libs.junit5.engine)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

benchmark {
    targets {
        register("jvm")
    }
    configurations {
        create("parsing") {
            include("nl.adaptivity.xmlutil.benchmark.Parsing")
        }
        create("deserialization") {
            include("nl.adaptivity.xmlutil.benchmark.Deserialization")
        }
        create("deserializationFast") {
            include("nl.adaptivity.xmlutil.benchmark.Deserialization.testDeserializeGenericSpeedRetainedXml")
            include("nl.adaptivity.xmlutil.benchmark.Deserialization.testDeserializeNoparseRetained")
        }
        create("serialization") {
            include("nl.adaptivity.xmlutil.benchmark.Serialization")
        }
    }
}

allOpen {
    annotations("org.openjdk.jmh.annotations.State", "kotlinx.benchmark.State")
}

//jmh {
//    jmhVersion = libs.versions.jmh.core
//}
