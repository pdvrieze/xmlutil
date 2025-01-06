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

@file:Suppress("PropertyName")

plugins {
    id("projectPlugin")
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
    idea
}

base {
    archivesName = "examples"
}

val autoModuleName = "net.devrieze.serialexamples"

dependencies {
    implementation(projects.serialization)
    implementation(projects.serialutil)
    testImplementation(projects.testutil)
}
