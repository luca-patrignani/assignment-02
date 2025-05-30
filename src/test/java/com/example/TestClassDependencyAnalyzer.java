package com.example;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class TestClassDependencyAnalyzer {
    protected abstract DepsReport getDependencies(String code);

    @Test
    void testProfessor() {
        var code = "";
        try {
            code = new String(Files.readAllBytes(Paths.get("src", "main", "java", "pcd", "ass02", "MyClass.java").toAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final var dependencies = getDependencies(code);
        assertEquals("pcd.ass02.MyClass", dependencies.name());
        assertEquals(Set.of("pcd.ass02.example", "pcd.ass02.foopack.D", "pcd.ass02.foopack2.E", "pcd.ass02.example.A", "pcd.ass02.foopack.B", "pcd.ass02.C"), dependencies.dependencies());

    }

    @Test
    void testInnerClassDependency() {
        var code = "";
        try {
            code = new String(Files.readAllBytes(Paths.get("src", "main", "java", "pcd", "ass02", "foopack2", "F.java").toAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final var dependencies = getDependencies(code);
        assertEquals("pcd.ass02.foopack2.F", dependencies.name());
        assertEquals(Set.of("pcd.ass02.foopack2.G", "pcd.ass02.foopack2.G.InnerG"), dependencies.dependencies());

    }

    @Test
    void testSamePackage() {
        var code = "";
        try {
            code = new String(Files.readAllBytes(Paths.get("src", "main", "java", "pcd", "ass02", "foopack3", "L.java").toAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final var dependencies = getDependencies(code);
        assertEquals("pcd.ass02.foopack3.L", dependencies.name());
        assertEquals(Set.of("pcd.ass02.foopack3.I"), dependencies.dependencies());

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
        assertEquals(Set.of("java.lang.Object", "com.example.A.B"), dependencies.dependencies());
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
        assertEquals(Set.of("B.A"), dependencies.dependencies());
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
        assertEquals(Set.of("com.example.D", "B.A"), dependencies.dependencies());
    }
}
