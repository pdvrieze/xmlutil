/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("android") apply false
}

description = "The overall project for cross-platform xml access"

ext {
    set("myJavaVersion",JavaVersion.VERSION_1_8)
}

task<Wrapper>("wrapper") {
    gradleVersion = "4.10.2"
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

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
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
