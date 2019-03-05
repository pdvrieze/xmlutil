# Versions
* License: [![GitHub license](https://img.shields.io/badge/License-Apache%202-blue.svg?style=flat)](COPYING)
* Core: [ ![Download](https://api.bintray.com/packages/pdvrieze/maven/xmlutil/images/download.svg) ](https://bintray.com/pdvrieze/maven/xmlutil/_latestVersion) 
* Serialization: [ ![Download](https://api.bintray.com/packages/pdvrieze/maven/xmlutil-serialization/images/download.svg) ](https://bintray.com/pdvrieze/maven/xmlutil-serialization/_latestVersion) 
* Build: [![Build Status](https://travis-ci.com/pdvrieze/xmlutil.svg?branch=master)](https://travis-ci.com/pdvrieze/xmlutil)
# Introduction

This project is a cross-platform XML serialization (wrapping) library compatible with kotlin serialization. It provides
capabilities for Android, JVM and JS (alpha quality)

It also provides serialization support

**Help wanted**: Any help with extending this project is welcome. Help is especially needed for the following aspects:

* Documentation updates
* Testing, in particular more extensive tests. Some tests already exist for both JVM and Android
* Javascript support
  * Core Javascript support needs testing but should work. It is based on DOM so may be slow
  * Javascript serialization support: make serialization work on Javascript once possible
* Native support: Currently there is no implementation for Kotlin native. 

# How to use
The library is designed as a multiplatform kotlin module, but platform-specific versions can also be used were appropriate.
## Core
### multiplatform
```
   implementation("net.devrieze:xmlutil:0.8.0")
```
### JVM -- uses the stax API not available on Android
```
   implementation("net.devrieze:xmlutil-jvm:0.8.0")
```
### Android -- Uses the android streaming library
```
   implementation("net.devrieze:xmlutil-android:0.8.0")
```
### JS -- Wraps DOM
```
   implementation("net.devrieze:xmlutil-js:0.8.0")
```
## Serialization
### multiplatform
```
   implementation("net.devrieze:xmlutil-serialization:0.8.0")
```
### JVM
```
   implementation("net.devrieze:xmlutil-serialization-jvm:0.8.0")
```
### Android
```
   implementation("net.devrieze:xmlutil-serialization-android:0.8.0")
```
### js
```
   implementation("net.devrieze:xmlutil-serialization-js:0.8.0")
```


## Modules

### core
Container for the core library (versions)

### core.common
All code shared between Javascript and Java (either jvm or android)

### core.common-nonshared
All code that is common, but not shared between Jvm and Android platforms

### core.android
Code specific to the Android platform (Pulls in core.java as API dependency). This is a regular jar rather than an AAR
as the only specific thing to Android is the XML library

### core.java
Implementation of the shared code for Java based platforms (both Android and JVM)

### core.js
Javascript based implementation

### core.jvm
Code unique to the JVM platform (Pulls in core.java as API dependency)

### Serialization
The kotlinx.serialization plugin to allow serialization to XML

### Serialization.java
The java version of the serialization plugin. Please note that it does not pull in the platform specific library. The
core library is dependent on the actual platform used (JVM or Android). This library only pulls in the shared Java code.

### Serialization.jvm
The JVM version merely uses the jvm platform xml library but the serialization is
### Serialization.android

### Serialization.js
The Javascript version of the serialization plugin. This is not yet implemented due to missing annotation support for
javascript and the 0.6.0 version of kotlinx.serialization not supporting type annotations.

### Serialization.test-android
An android test project to test serialization on Android.
