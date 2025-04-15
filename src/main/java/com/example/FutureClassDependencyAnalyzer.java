package com.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.vertx.core.Future;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class FutureClassDependencyAnalyzer {
    public FutureClassDependencyAnalyzer() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    public Future<DepsReport> getClassDependencies(Future<InputStream> classCode) {
        Future<CompilationUnit> compilationUnit = classCode
                .compose(code -> Future.succeededFuture(StaticJavaParser.parse(code)));
        Future<Set<String>> usedTypesFuture = compilationUnit.compose(
                cu -> Future.succeededFuture(
                        cu.findAll(ClassOrInterfaceType.class).stream()
                                .map(ClassOrInterfaceType::getNameAsString)
                                .collect(toSet())
                )
        );
        Future<Set<ClassOrInterfaceDeclaration>> declaredTypesFuture = compilationUnit.compose(
                cu -> Future.succeededFuture(new HashSet<>(cu.findAll(ClassOrInterfaceDeclaration.class)))
        );
        Future<Map<ClassVisibility, Set<String>>> classesForVisibility = declaredTypesFuture
                .compose(classOrInterfaceDeclarations ->
                    Future.succeededFuture(
                    classOrInterfaceDeclarations.stream()
                            .collect(Collectors.groupingBy(ClassVisibility::fromClassDeclaration,
                                    mapping(ClassOrInterfaceDeclaration::getNameAsString, toSet())))
                    )
                );

        return Future.all(usedTypesFuture, classesForVisibility)
                .compose(compositeFuture -> {
                    @SuppressWarnings("unchecked")
                    final var dependencyTypes = (Set<String>)compositeFuture.resultAt(0);
                    @SuppressWarnings("unchecked")
                    final var declaredTypes = (Map<ClassVisibility, Set<String>>)compositeFuture.resultAt(1);
                    for (final Set<String> classDecSet : declaredTypes.values()) {
                        dependencyTypes.removeAll(classDecSet);
                    }
                    return Future.succeededFuture(new DepsReport(
                            declaredTypes.get(ClassVisibility.PUBLIC),
                            declaredTypes.get(ClassVisibility.PROTECTED),
                            declaredTypes.get(ClassVisibility.PACKAGE_PROTECTED),
                            dependencyTypes
                    ));
                });
    }

    public enum ClassVisibility {
        PUBLIC, PROTECTED, PACKAGE_PROTECTED, PRIVATE;
        static ClassVisibility fromClassDeclaration(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
            NodeList<Modifier> classModifiers = classOrInterfaceDeclaration.getModifiers();
            if (classModifiers.contains(Modifier.publicModifier())) {
                return ClassVisibility.PUBLIC;
            }
            if (classModifiers.contains(Modifier.protectedModifier())) {
                return ClassVisibility.PROTECTED;
            }
            if (classModifiers.contains(Modifier.privateModifier())) {
                return ClassVisibility.PRIVATE;
            }
            return ClassVisibility.PACKAGE_PROTECTED;
        }
    }

}
