package com.example.future;

import com.example.DepsReport;
import com.example.TestClassDependencyAnalyzer;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

import java.nio.file.Path;

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
