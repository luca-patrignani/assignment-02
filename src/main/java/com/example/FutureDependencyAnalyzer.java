package com.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.vertx.core.Future;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FutureDependencyAnalyzer {
    public Future<Set<String>> getClassDependencies(Future<InputStream> classCode) {
        Future<CompilationUnit> compilationUnit = classCode
                .compose(code -> {
                    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
                    combinedTypeSolver.add(new ReflectionTypeSolver());
                    JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
                    StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
                    return Future.succeededFuture(StaticJavaParser.parse(code));
                });
        Future<Set<String>> usedTypes = compilationUnit.compose(
                cu -> Future.succeededFuture(
                        cu.findAll(ClassOrInterfaceType.class).stream()
                                .map(ClassOrInterfaceType::getNameAsString)
                                .collect(Collectors.toSet())
                )
        );
        Future<Set<String>> declaredTypes = compilationUnit.compose(
                cu -> Future.succeededFuture(
                        cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                                .map(ClassOrInterfaceDeclaration::getNameAsString)
                                .collect(Collectors.toSet())
                )
        );
        return Future.all(usedTypes, declaredTypes)
                .compose(compositeFuture -> {
                    final var s1 = (Set<String>)compositeFuture.resultAt(0);
                    final var s2 = (Set<String>)compositeFuture.resultAt(1);
                    s1.removeAll(s2);
                    return Future.succeededFuture(s1);
                });
    }

}
