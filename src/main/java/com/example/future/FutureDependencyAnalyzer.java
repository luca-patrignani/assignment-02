package com.example.future;

import com.example.DepsReport;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FutureDependencyAnalyzer {
    final Path rootDirectory;
    final FuturePackageDependencyAnalyzer pda;
    final FutureClassDependencyAnalyzer cda;

    public FutureDependencyAnalyzer(Path rootDirectory) {
        this.rootDirectory = rootDirectory.toAbsolutePath();
        this.cda = new FutureClassDependencyAnalyzer(rootDirectory);
        this.pda = new FuturePackageDependencyAnalyzer(rootDirectory);
    }

    public Future<DepsReport> getClassDependencies(Path classPath) {
        var code = "";
        try {
            code = new String(Files.readAllBytes(classPath.toAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return cda.getClassDependencies(Future.succeededFuture(Buffer.buffer(code)));
    }

    public Future<DepsReport> getPackageDependencies(Path packagePath) {
        return pda.getPackageDependencies(Future.succeededFuture(packagePath.toAbsolutePath()));
    }

    public Future<DepsReport> getProjectDependencies() {
        return pda.getPackageDependencies(Future.succeededFuture(rootDirectory));
    }
}
