module io.github.pdvrieze.testutil {
    requires transitive net.devrieze.xmlutil.core;
    requires kotlin.test.junit5;
    requires java.xml;
//    requires org.junit.jupiter.api;

    exports nl.adaptivity.xmlutil.test;
    exports io.github.pdvrieze.xmlutil.testutil;
}
