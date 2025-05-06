package com.example.rx;

import com.example.DepsReport;
import com.example.TestClassDependencyAnalyzer;
import io.reactivex.rxjava3.core.Flowable;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class TestRxClassDependencyAnalyzer extends TestClassDependencyAnalyzer {

    private final RxClassDependencyAnalyzer cda = new RxClassDependencyAnalyzer(Path.of("src","main", "java").toAbsolutePath());
    @Override
    protected DepsReport getDependencies(String code) {
        return cda.getClassDependencies(Flowable.just(code)).blockingFirst();
    }
}
