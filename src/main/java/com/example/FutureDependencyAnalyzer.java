package com.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.vertx.core.Future;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

public class FutureDependencyAnalyzer {
    public FutureDependencyAnalyzer() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    public Future<Set<String>> getClassDependencies(Future<InputStream> classCode) {
        Future<CompilationUnit> compilationUnit = classCode
                .compose(code -> Future.succeededFuture(StaticJavaParser.parse(code)));
        Future<Set<String>> usedTypesFuture = compilationUnit.compose(
                cu -> Future.succeededFuture(
                        cu.findAll(ClassOrInterfaceType.class).stream()
                                .map(ClassOrInterfaceType::getNameAsString)
                                .collect(Collectors.toSet())
                )
        );
        Future<Set<String>> declaredTypesFuture = compilationUnit.compose(
                cu -> Future.succeededFuture(
                        cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                                .map(ClassOrInterfaceDeclaration::getNameAsString)
                                .collect(Collectors.toSet())
                )
        );
        return Future.all(usedTypesFuture, declaredTypesFuture)
                .compose(compositeFuture -> {
                    @SuppressWarnings("unchecked")
                    final var usedTypes = (Set<String>)compositeFuture.resultAt(0);
                    @SuppressWarnings("unchecked")
                    final var declaredTypes = (Set<String>)compositeFuture.resultAt(1);
                    usedTypes.removeAll(declaredTypes);
                    return Future.succeededFuture(usedTypes);
                });
    }

}
