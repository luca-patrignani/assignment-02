package com.example;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestPackageDependencyAnalyzer {


    private final FuturePackageDependencyAnalyzer pda = new FuturePackageDependencyAnalyzer(Path.of("src","main", "java").toAbsolutePath());

    @Test
    void testProfessor() {
        var packagePath = Paths.get("src","main","java","pcd","ass02","foopack").toAbsolutePath();

        var dependencies = getDependencies(packagePath);
        assertEquals("pcd.ass02.foopack",dependencies.name());
        assertEquals(Set.of("pcd.ass02.MyClass","pcd.ass02.foopack.D"),dependencies.dependencies());

    }

    private DepsReport getDependencies(Path packagePath) {
        final var dependencies = pda.getPackageDependencies(Future.succeededFuture(packagePath));
        final var result = dependencies.result();
        assertNull(dependencies.cause());
        return result;
    }
}
