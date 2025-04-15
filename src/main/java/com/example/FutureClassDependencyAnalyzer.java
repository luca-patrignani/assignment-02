package com.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
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
import java.util.stream.Stream;

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
                        Stream.concat(
                                        cu.findAll(ClassOrInterfaceType.class).stream(),
                                        cu.findAll(EnumDeclaration.class).stream()
                                )
                                .map(NodeWithSimpleName::getNameAsString)
                                .collect(toSet())
                )
        );
        Future<Set<TypeDeclaration<?>>> declaredTypesFuture = compilationUnit.compose(cu -> {
                    final var classDec = new HashSet<TypeDeclaration<?>>(cu.findAll(ClassOrInterfaceDeclaration.class));
                    final var enumDec = new HashSet<>(cu.findAll(EnumDeclaration.class));
                    classDec.addAll(enumDec);
                    return Future.succeededFuture(classDec);
                }
        );
        Future<Map<ClassVisibility, Set<String>>> classesForVisibility = declaredTypesFuture
                .compose(classOrInterfaceDeclarations ->
                        Future.succeededFuture(
                                classOrInterfaceDeclarations.stream()
                                        .collect(Collectors.groupingBy(ClassVisibility::fromClassDeclaration,
                                                mapping(TypeDeclaration::getNameAsString, toSet())))
                        )
                );

        return Future.all(usedTypesFuture, classesForVisibility)
                .compose(compositeFuture -> {
                    @SuppressWarnings("unchecked") final var dependencyTypes = (Set<String>) compositeFuture.resultAt(0);
                    @SuppressWarnings("unchecked") final var declaredTypes = (Map<ClassVisibility, Set<String>>) compositeFuture.resultAt(1);
                    for (final Set<String> classDecSet : declaredTypes.values()) {
                        dependencyTypes.removeAll(classDecSet);
                    }
                    return Future.succeededFuture(new DepsReport(
                            declaredTypes.getOrDefault(ClassVisibility.PUBLIC, Set.of()),
                            declaredTypes.getOrDefault(ClassVisibility.PROTECTED, Set.of()),
                            declaredTypes.getOrDefault(ClassVisibility.PACKAGE_PROTECTED, Set.of()),
                            dependencyTypes
                    ));
                });
    }

    public enum ClassVisibility {
        PUBLIC, PROTECTED, PACKAGE_PROTECTED, PRIVATE;

        static ClassVisibility fromClassDeclaration(TypeDeclaration<?> classOrInterfaceDeclaration) {
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
