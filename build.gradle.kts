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

import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
    idea
    kotlin("android") apply false
    id("maven-publish") apply false
}

description = "The overall project for cross-platform xml access"

ext {
    set("myJavaVersion",JavaVersion.VERSION_1_8)
}

tasks {
    (findByName("wrapper") as? Wrapper)?.run {
        gradleVersion = "6.4.1"
    }
}

val xmlutil_version: String by project

allprojects {
    group = "io.github.pdvrieze.xmlutil"
    version = xmlutil_version

}
subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven {
            name="GitHubPackages"
            url = uri("https://maven.pkg.github.com/pdvrieze/xmlutil")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }

}

configurations.all {
    resolutionStrategy {
        force("org.apache.httpcomponents:httpclient:4.5.9")
        force("org.apache.httpcomponents:httpcore:4.4.11")
    }
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishOnFailureIf("true".equals(System.getenv("TRAVIS")))
}

idea {
    project {
        languageLevel = IdeaLanguageLevel(JavaVersion.VERSION_1_8)
    }
    module {
        isDownloadSources=true
        contentRoot = projectDir
    }
}
