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
    id("com.jfrog.bintray") apply false
    id("com.gradle.build-scan") version "2.3"
}

description = "The overall project for cross-platform xml access"

ext {
    set("myJavaVersion",JavaVersion.VERSION_1_8)
}

tasks {
    (findByName("wrapper") as? Wrapper)?.run {
        gradleVersion = "5.4.1"
    }
}

allprojects {
    repositories {
        mavenLocal()
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
        google()

        maven("https://kotlin.bintray.com/kotlinx")

        maven("https://dl.bintray.com/jetbrains/spek")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
/*

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
 */
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
