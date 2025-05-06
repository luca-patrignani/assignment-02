package com.example;

import io.reactivex.rxjava3.core.Flowable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class TestRxPackageDependencyAnalyzer extends TestPackageDependencyAnalyzer {
    private final RxDependencyAnalyzer pda = new RxDependencyAnalyzer(Path.of("src","main", "java").toAbsolutePath());
    @Override
    protected DepsReport getDependencies(Path packagePath) {
        RxDepsReport result;
        result = pda.getPackageDependencies(packagePath).blockingFirst();
        Set<String> dependencies = new HashSet<>();
        result.dependencies().blockingSubscribe(dependencies::add);
        return new DepsReport(result.name(),dependencies);
    }
}
