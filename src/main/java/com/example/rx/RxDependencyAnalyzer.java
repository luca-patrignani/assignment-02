package com.example.rx;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RxDependencyAnalyzer {
    final Path rootDirectory;
    final RxPackageDependencyAnalyzer pda;
    final RxClassDependencyAnalyzer cda;

    public RxDependencyAnalyzer(Path rootDirectory) {
        this.rootDirectory = rootDirectory.toAbsolutePath();
        this.cda = new RxClassDependencyAnalyzer(rootDirectory);
        this.pda = new RxPackageDependencyAnalyzer(rootDirectory);
    }

    public Flowable<RxDepsReport> getClassDependencies(Path classPath) {
        return cda.getClassDependencies(Flowable.fromCallable(() -> Files.readString(classPath)).subscribeOn(Schedulers.io()));
    }

    public Flowable<RxDepsReport> getPackageDependencies(Path packagePath) {
        try {
            return pda.getPackageDependencies(packagePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public Flowable<RxDepsReport> getProjectDependencies() {
        try {
            return pda.getPackageDependencies(rootDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
