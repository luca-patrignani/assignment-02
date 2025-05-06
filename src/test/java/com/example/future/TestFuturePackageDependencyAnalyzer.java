package com.example.future;

import com.example.DepsReport;
import com.example.TestPackageDependencyAnalyzer;
import io.vertx.core.Future;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;

public class TestFuturePackageDependencyAnalyzer extends TestPackageDependencyAnalyzer {
    private final FutureDependencyAnalyzer pda = new FutureDependencyAnalyzer(Path.of("src","main", "java").toAbsolutePath());

    @Override
    protected DepsReport getDependencies(Path packagePath) {
        final var dependencies = pda.getPackageDependencies(packagePath);
        // waiting for the future completion, who cares if it's blocking
        final DepsReport result = Future.await(dependencies);
        assertNull(dependencies.cause());
        return result;
    }

}
