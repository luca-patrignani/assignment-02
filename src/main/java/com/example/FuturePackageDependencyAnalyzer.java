package com.example;

import io.vertx.core.Future;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FuturePackageDependencyAnalyzer {

    public Future<Set<String>> getPackageDependencies(List<Future<Set<String>>> packageFiles) {
        return FuturesHelper.all(packageFiles)
                .compose(sets -> null);
    }

//    private Set<String> findDependencies(List<Set<String>> files) {
//        final var result = new HashSet<>();
//        for (final var file : files) {
//
//        }
//    }
}
