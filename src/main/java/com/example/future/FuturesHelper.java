package com.example.future;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import java.util.List;
import java.util.function.BiFunction;

public class FuturesHelper {
    public static <A> Future<List<A>> all(List<Future<A>> futures) {
        //noinspection unchecked
        return Future.all(futures)
                .map(CompositeFuture::list)
                .map(list -> list.stream()
                        .map(a -> (A) a)
                        .toList()
                );
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C> Future<C> all(Future<A> a, Future<B> b, BiFunction<A, B, Future<C>> mapper) {
        return Future.all(a, b)
                .compose(compositeFuture -> {
                    final var aa = (A) compositeFuture.resultAt(0);
                    final var bb = (B) compositeFuture.resultAt(1);
                    return mapper.apply(aa, bb);
                });
    }
}
