package com.example;

import java.util.Set;

public record DepsReport(String name, Set<String> dependencies) {
    public String getPackage() {
        var fqn = name();

        int lastDot = fqn.lastIndexOf('.');
        if (lastDot == -1) {
            // No dot: could be a top-level class or default package
            return fqn;
        }

        String tail = fqn.substring(lastDot + 1);
        // Check if tail starts with an uppercase letter (likely a class name)
        if (Character.isUpperCase(tail.charAt(0))) {
            return fqn.substring(0, lastDot); // It's a class: return package
        } else {
            return fqn; // Likely a package name already
        }
    }
}


