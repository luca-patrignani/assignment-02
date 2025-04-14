import com.example.FutureDependencyAnalyzer;
import io.vertx.core.Future;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFutureDependencyAnalyzer {

    final FutureDependencyAnalyzer fda = new FutureDependencyAnalyzer();

    private Set<String> getDependencies(String code) {
        return fda.getClassDependencies(Future.succeededFuture(new ByteArrayInputStream(code.getBytes())))
                .result();
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
        final Set<String> dependencies = getDependencies(code);
        assertEquals(Set.of("C", "String"), dependencies);
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
        final Set<String> dependencies = getDependencies(code);
        assertEquals(Set.of("Object"), dependencies);
    }
}
