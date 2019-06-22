module net.devrieze.xmlutil.core {
    requires java.base;
    requires transitive java.xml;
    requires kotlin.stdlib;
    requires kotlinx.serialization.runtime;
    requires net.devrieze.serialutil;
    exports nl.adaptivity.xml;
    exports nl.adaptivity.xmlutil;
    exports nl.adaptivity.xmlutil.core.impl.multiplatform to net.devrieze.xmlutil.serialization;
    exports nl.adaptivity.xmlutil.util;
}