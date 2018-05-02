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

plugins {
    `java-library`
    java
    id("kotlin-platform-jvm")
}

base {
    archivesBaseName="PE-diagram"
}

java.sourceSets {
    create("imageGen").apply {
        java.srcDir("src/imagegen/java")
    }
}

val imageGenCompile = configurations["imageGenCompile"].apply { extendsFrom(configurations["apiElements"]) }
val imageGenRuntime = configurations["imageGenRuntime"].apply { extendsFrom(configurations["runtimeElements"]) }

val jupiterVersion: String by project

dependencies {
    expectedBy(project(":PE-diagram:common"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":PE-common:jvm"))
    compileOnly(project(path= ":PE-common:jvm", configuration="compileOnly"))
    imageGenCompile(project(":PE-diagram:jvm"))
    imageGenRuntime(project (":xmlutil:core:jvm"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
