package com.example;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestFutureClassDependencyAnalyzer extends TestClassDependencyAnalyzer {

    final FutureClassDependencyAnalyzer fda = new FutureClassDependencyAnalyzer(Path.of("src","main", "java").toAbsolutePath());

    @Override
    protected DepsReport getDependencies(String code) {
        final var dependencies = fda.getClassDependencies(Future.succeededFuture(Buffer.buffer(code)));
        final var result = dependencies.result();
        assertNull(dependencies.cause());
        return result;
    }


}
