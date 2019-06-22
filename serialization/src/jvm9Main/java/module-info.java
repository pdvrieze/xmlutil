module net.devrieze.xmlutil.serialization {
    requires java.base;
    requires transitive java.xml;
    requires kotlin.stdlib;
    requires kotlinx.serialization.runtime;
    requires net.devrieze.xmlutil.core;
//    requires net.devrieze.serialutil;
    exports nl.adaptivity.xmlutil.serialization;
    exports nl.adaptivity.xmlutil.serialization.compat;
    exports nl.adaptivity.xmlutil.serialization.canary;
}