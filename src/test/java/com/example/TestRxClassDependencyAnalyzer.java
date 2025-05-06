package com.example;

import io.reactivex.rxjava3.core.Flowable;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class TestRxClassDependencyAnalyzer extends TestClassDependencyAnalyzer {

    private final RxClassDependencyAnalyzer cda = new RxClassDependencyAnalyzer(Path.of("src","main", "java").toAbsolutePath());
    @Override
    protected DepsReport getDependencies(String code) {
        var result = cda.getClassDependencies(Flowable.just(code)).blockingFirst();
        Set<String> dependencies = new HashSet<>();
        result.dependencies().blockingSubscribe(dependencies::add);
        return new DepsReport(result.name(),dependencies);
    }
}
