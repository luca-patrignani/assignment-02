package com.example;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPackageDependencyAnalyzer {

    private final FutureClassDependencyAnalyzer cda = new FutureClassDependencyAnalyzer();

    @Test
    void testTwoFilePackage() {
        final var file1 = """
            public class A {
                public void method() {
                    final C b = new B();
                }
            }
        """;
        final var dependencies1 = getClassDependencies(file1);
        final var file2 = """
            public class B {
            }
        """;
        final var dependencies2 = getClassDependencies(file2);
        final var pda = new FuturePackageDependencyAnalyzer();
//        final var dependencies = pda.getPackageDependencies(List.of(dependencies1, dependencies2)).result();
//        assertEquals(Set.of("C"), dependencies);
    }

    private Future<DepsReport> getClassDependencies(String file) {
        return cda.getClassDependencies(Future.succeededFuture(new ByteArrayInputStream(file.getBytes())));
    }
}
