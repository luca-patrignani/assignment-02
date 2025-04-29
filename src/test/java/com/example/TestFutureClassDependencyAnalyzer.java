package com.example;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestFutureClassDependencyAnalyzer {

    final FutureClassDependencyAnalyzer fda = new FutureClassDependencyAnalyzer(Path.of("src","main", "java").toAbsolutePath());

    private DepsReport getDependencies(String code) {
        final var dependencies = fda.getClassDependencies(Future.succeededFuture(new ByteArrayInputStream(code.getBytes())));
        final var result = dependencies.result();
        assertNull(dependencies.cause());
        return result;
    }

    @Test
    void testSimple() {
        var code = "";
        try {
            code = new String(Files.readAllBytes(Paths.get("src","main","java","pcd","ass02","MyClass.java").toAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final var dependencies = getDependencies(code);
        assertEquals("pcd.ass02.MyClass", dependencies.name());
        assertEquals(Set.of("pcd.ass02.example", "pcd.ass02.foopack.D", "pcd.ass02.foopack2.E", "pcd.ass02.example.A", "pcd.ass02.foopack.B", "pcd.ass02.C"), dependencies.dependencies());

    }

    @Test
    void testClassDeclaredLocally() {
        final String code = """
                    package com.example;
                
                    public class A {
                        public class B {}
                
                        public void method() {
                            final Object b = new B();
                        }
                    }
                """;
        final var dependencies = getDependencies(code);
        assertEquals("com.example.A", dependencies.name());
        assertEquals(Set.of("java.lang.Object"), dependencies.dependencies());
    }

    @Test
    void testInterface() {
        final String code = """
                    package com.example;
                
                    public interface A {
                        void method(Integer i);
                    }
                """;
        final var dependencies = getDependencies(code);
        assertEquals("com.example.A", dependencies.name());
        assertEquals(Set.of("java.lang.Integer"), dependencies.dependencies());
    }

    @Test
    void testClassRecursive() {
        final String code = """
                    package com.example;
                
                    public class A {
                        public void method(A a) {
                            return this;
                        }
                    }
                """;
        final var dependencies = getDependencies(code);
        assertEquals("com.example.A", dependencies.name());
        assertEquals(Set.of(), dependencies.dependencies());
    }

    @Test
    void testClassExtension() {
        final String code = """
                    package com.example;
                
                    public class A extends Object {
                        public void method(A a) {
                            return this;
                        }
                    }
                """;
        final var dependencies = getDependencies(code);
        assertEquals("com.example.A", dependencies.name());
        assertEquals(Set.of("java.lang.Object"), dependencies.dependencies());
    }

    @Test
    void testProtectedInnerClass() {
        final String code = """
                    package com.example;
                
                    public class A {
                        protected class B {}
                        public void method() {
                        }
                    }
                """;
        final var dependencies = getDependencies(code);
        assertEquals("com.example.A", dependencies.name());
        assertEquals(Set.of(), dependencies.dependencies());
    }

    @Test
    void testPackageProtectedClass() {
        final String code = """
                    package com.example;
                
                    class A {
                    }
                """;
        final var dependencies = getDependencies(code);
        assertEquals("com.example.A", dependencies.name());
        assertEquals(Set.of(), dependencies.dependencies());
    }

    @Test
    void testPrivateInnerClass() {
        final String code = """
                    package com.example;
                
                    public class A {
                        private class B {}
                        public void method() {
                        }
                    }
                """;
        final var dependencies = getDependencies(code);
        assertEquals("com.example.A", dependencies.name());
        assertEquals(Set.of(), dependencies.dependencies());
    }

    @Test
    void testEnum() {
        final String code = """
                    public enum A {}
                """;
        final var dependencies = getDependencies(code);
        assertEquals("A", dependencies.name());
        assertEquals(Set.of(), dependencies.dependencies());
    }

    @Test
    void testRecord() {
        final String code = """
                    public class B {
                        public record A() {};
                        final A a = new A();
                    }
                """;
        final var dependencies = getDependencies(code);
        assertEquals("B", dependencies.name());
        assertEquals(Set.of(), dependencies.dependencies());
    }

    @Test
    void testImportNotUsed() {
        final String code = """
                    import com.example.D;
                
                    public class B {
                        public record A() {};
                        final A a = new A();
                    }
                """;
        final var dependencies = getDependencies(code);
        assertEquals("B", dependencies.name());
        assertEquals(Set.of("com.example.D"), dependencies.dependencies());
    }

}
