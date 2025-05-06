package com.example;

import io.reactivex.rxjava3.core.Flowable;

public record RxDepsReport(String name, Flowable<String> dependencies) {
}
