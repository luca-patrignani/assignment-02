package com.example;

import java.util.Set;

public record DepsReport(Set<String> publicTypes, Set<String> protectedTypes, Set<String> packageProtectedTypes, Set<String> dependencies) {
}
