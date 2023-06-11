module net.devrieze.xmlutil.core {
    requires transitive kotlin.stdlib;
    requires java.xml;
    requires kotlinx.serialization.core;

    exports nl.adaptivity.xmlutil;
    exports nl.adaptivity.xmlutil.core;
    exports nl.adaptivity.xmlutil.dom;
    exports nl.adaptivity.xmlutil.util;
    exports nl.adaptivity.xmlutil.util.impl to net.devrieze.xmlutil.serialization;
    exports nl.adaptivity.xmlutil.core.internal to net.devrieze.xmlutil.serialization;
    exports nl.adaptivity.xmlutil.core.impl to net.devrieze.xmlutil.serialization;
    exports nl.adaptivity.xmlutil.core.impl.multiplatform to net.devrieze.xmlutil.serialization, net.devrieze.xmlutil.xmlserializable;
    provides nl.adaptivity.xmlutil.XmlStreamingFactory with nl.adaptivity.xmlutil.StAXStreamingFactory;
    provides nl.adaptivity.xmlutil.util.SerializationProvider with  nl.adaptivity.xmlutil.util.DefaultSerializationProvider;
}
