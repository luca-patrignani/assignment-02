package com.example.rx;

import com.example.DepsReport;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RxPackageDependencyAnalyzer {

    private final RxClassDependencyAnalyzer cda;

    public RxPackageDependencyAnalyzer(final Path rootDirectory) {
        cda = new RxClassDependencyAnalyzer(rootDirectory.toAbsolutePath());
    }

    public Flowable<DepsReport> getPackageDependencies(Path packagePath) throws IOException {
        return cda.getClassDependencies(Flowable.fromStream(Files.walk(packagePath))
                        .filter(Files::isRegularFile)
                        .map(Files::readString))
                .subscribeOn(Schedulers.io());
    }
}
