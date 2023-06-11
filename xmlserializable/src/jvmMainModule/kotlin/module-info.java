module net.devrieze.xmlutil.xmlserializable {
    requires transitive kotlin.stdlib;
    requires kotlinx.serialization.core;
    requires java.xml;
    requires net.devrieze.xmlutil.core;

    exports nl.adaptivity.xmlutil.xmlserializable;
}
