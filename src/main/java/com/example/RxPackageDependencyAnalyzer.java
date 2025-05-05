package com.example;

import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.Future;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collector;

public class RxPackageDependencyAnalyzer {

    private final RxClassDependencyAnalyzer cda;

    public RxPackageDependencyAnalyzer(final Path rootDirectory) {
        cda = new RxClassDependencyAnalyzer(rootDirectory.toAbsolutePath());
    }

    public Flowable<DepsReport> getPackageDependencies(Path packagePath) throws IOException {
        final var reports = cda.getClassDependencies(
                Flowable.fromStream(Files.walk(packagePath))
                        .map(Files::readString)
        );
//        final var classNames = reports
//                .map(DepsReport::name)
//                .collect(Collector.of())
        return null;
    }
}
