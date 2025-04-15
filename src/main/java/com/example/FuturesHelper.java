package com.example;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import java.util.List;

public class FuturesHelper {
    public static <A> Future<List<A>> all(List<Future<A>> futures) {
        //noinspection unchecked
        return Future.all(futures)
                .map(CompositeFuture::list)
                .map(list -> list.stream()
                        .map(a -> (A)a)
                        .toList()
                );
    }
}
