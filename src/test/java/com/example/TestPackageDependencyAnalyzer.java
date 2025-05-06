package com.example;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class TestPackageDependencyAnalyzer {

    protected abstract DepsReport getDependencies(Path packagePath);

    @Test
    void testProfessorFoopack() {
        var packagePath = Paths.get("src","main","java","pcd","ass02","foopack").toAbsolutePath();

        var dependencies = getDependencies(packagePath);
        assertEquals("pcd.ass02.foopack",dependencies.name());
        assertEquals(Set.of(), dependencies.dependencies());
    }

    @Test
    void testProfessorFoopack2() {
        var packagePath = Paths.get("src","main","java","pcd","ass02","foopack2").toAbsolutePath();

        var dependencies = getDependencies(packagePath);
        assertEquals("pcd.ass02.foopack2",dependencies.name());
        assertEquals(Set.of("pcd.ass02.foopack.D", "pcd.ass02.MyClass"), dependencies.dependencies());
    }

    @Test
    void testProfessorFoopack3() {
        var packagePath = Paths.get("src","main","java","pcd","ass02","foopack3").toAbsolutePath();

        var dependencies = getDependencies(packagePath);
        assertEquals("pcd.ass02.foopack3",dependencies.name());
        assertEquals(Set.of("java.lang.Integer"), dependencies.dependencies());
    }

    @Test
    void testProjectDependency() {
        var packagePath = Paths.get("src","main","java","pcd","ass02").toAbsolutePath();
        var dependencies = getDependencies(packagePath);
        assertEquals("pcd.ass02",dependencies.name());
        assertEquals(Set.of("java.lang.Integer"), dependencies.dependencies());
    }

}
