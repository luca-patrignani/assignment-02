package com.example;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestFutureClassDependencyAnalyzer {

    final FutureClassDependencyAnalyzer fda = new FutureClassDependencyAnalyzer();

    private DepsReport getDependencies(String code) {
        final var dependencies = fda.getClassDependencies(Future.succeededFuture(new ByteArrayInputStream(code.getBytes())));
        final var result = dependencies.result();
        assertNull(dependencies.cause());
        return result;
    }

    @Test
    void testClassNotDeclared() {
        final String code = """
            package com.example;
    
            public class Main {
                public static void main(String[] args) {
                    final var c = new C();
                    System.out.println("Hello, World!");
                }
            }
        """;
        final var dependencies = getDependencies(code);
        assertEquals(Set.of("C", "String"), dependencies.dependencies());
        assertEquals(Set.of("Main"), dependencies.publicTypes());
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
        assertEquals(Set.of("Object"), dependencies.dependencies());
        assertEquals(Set.of("A", "B"), dependencies.publicTypes());
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
        assertEquals(Set.of("Integer"), dependencies.dependencies());
        assertEquals(Set.of("A"), dependencies.publicTypes());
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
        assertEquals(Set.of(), dependencies.dependencies());
        assertEquals(Set.of("A"), dependencies.publicTypes());
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
        assertEquals(Set.of("Object"), dependencies.dependencies());
        assertEquals(Set.of("A"), dependencies.publicTypes());
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
        assertEquals(Set.of(), dependencies.dependencies());
        assertEquals(Set.of("B"), dependencies.protectedTypes());
    }

    @Test
    void testPackageProtectedClass() {
        final String code = """
            package com.example;
            
            class A {
            }
        """;
        final var dependencies = getDependencies(code);
        assertEquals(Set.of(), dependencies.dependencies());
        assertEquals(Set.of(), dependencies.publicTypes());
        assertEquals(Set.of("A"), dependencies.packageProtectedTypes());
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
        assertEquals(Set.of("A"), dependencies.publicTypes());
        assertEquals(Set.of(), dependencies.protectedTypes());
        assertEquals(Set.of(), dependencies.packageProtectedTypes());
        assertEquals(Set.of(), dependencies.dependencies());
    }
}
