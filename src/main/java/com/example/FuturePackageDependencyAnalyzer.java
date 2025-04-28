package com.example;

import io.vertx.core.Future;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FuturePackageDependencyAnalyzer {

    public Future<DepsReport> getPackageDependencies(List<Future<DepsReport>> packageFiles) {
        return FuturesHelper.all(packageFiles)
                .compose(reports -> {
                    final var publicTypes = reports.stream()
                            .map(DepsReport::publicTypes)
                            .flatMap(Set::stream)
                            .collect(Collectors.toSet());
                    final var dependencies = reports.stream()
                            .map(DepsReport::dependencies)
                            .flatMap(Set::stream)
                            .collect(Collectors.toSet());
                    dependencies.removeAll(publicTypes);
                    final var depsReport = new DepsReport(
                            publicTypes,
                            null,
                            null,
                            dependencies
                    );
                    return Future.succeededFuture(depsReport);
                });
    }

}
