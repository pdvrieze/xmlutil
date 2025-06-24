module net.devrieze.xmlutil.core {
    requires transitive kotlin.stdlib;
    requires kotlinx.serialization.core;
    requires java.xml;

    exports nl.adaptivity.xmlutil;
    exports nl.adaptivity.xmlutil.core;
    exports nl.adaptivity.xmlutil.dom;
    exports nl.adaptivity.xmlutil.dom2;
    exports nl.adaptivity.xmlutil.util;
}
