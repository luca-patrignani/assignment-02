package com.example;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestPackageDependencyAnalyzer {


    private final FuturePackageDependencyAnalyzer pda = new FuturePackageDependencyAnalyzer(Path.of("src","main", "java").toAbsolutePath());

    @Test
    void testProfessorFoopack() {
        var packagePath = Paths.get("src","main","java","pcd","ass02","foopack").toAbsolutePath();

        var dependencies = getDependencies(packagePath);
        assertEquals("pcd.ass02.foopack",dependencies.name());
        assertEquals(Set.of(), dependencies.dependencies());
    }

    @Test
    void testProfessorFoopack2() {
        var packagePath = Paths.get("src","main","java","pcd","ass02","foopack2").toAbsolutePath();

        var dependencies = getDependencies(packagePath);
        assertEquals("pcd.ass02.foopack2",dependencies.name());
        assertEquals(Set.of("pcd.ass02.foopack.D", "pcd.ass02.MyClass"), dependencies.dependencies());
    }

    @Test
    void testProfessorFoopack3() {
        var packagePath = Paths.get("src","main","java","pcd","ass02","foopack3").toAbsolutePath();

        var dependencies = getDependencies(packagePath);
        assertEquals("pcd.ass02.foopack3",dependencies.name());
        assertEquals(Set.of(), dependencies.dependencies());
    }

    private DepsReport getDependencies(Path packagePath) {
        final var dependencies = pda.getPackageDependencies(Future.succeededFuture(packagePath));
        // waiting for the future completion, who cares if it's blocking
        final DepsReport result = Future.await(dependencies);
        assertNull(dependencies.cause());
        return result;
    }
}
