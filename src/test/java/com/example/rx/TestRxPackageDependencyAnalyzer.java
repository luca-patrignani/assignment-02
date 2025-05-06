package com.example.rx;

import com.example.DepsReport;
import com.example.TestPackageDependencyAnalyzer;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class TestRxPackageDependencyAnalyzer extends TestPackageDependencyAnalyzer {
    private final RxDependencyAnalyzer pda = new RxDependencyAnalyzer(Path.of("src","main", "java").toAbsolutePath());
    @Override
    protected DepsReport getDependencies(Path packagePath) {
        return pda.getPackageDependencies(packagePath).blockingFirst();
    }
}
