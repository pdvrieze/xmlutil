/*
 * Copyright (c) 2016.
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

pluginManagement {
    includeBuild("project-plugins")
    repositories {
//        maven {
//            name = "Bundled maven"
//            url = file("mavenBundled")
//        }
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.library",
                "com.android.application" -> {
                    val ver = requested.version ?: "8.0.2"
                    useModule("com.android.tools.build:gradle:${ver}");
                }
            }
        }
    }
}

plugins {
    id ("com.gradle.enterprise") version "3.1.1"
}

rootProject.name = "xmlutil"

include(":serialutil")
include(":core")
include(":serialization")
include(":xmlserializable")
include(":testutil")
include(":examples")


gradleEnterprise {
    buildScan {
        // plugin configuration
    }
}
