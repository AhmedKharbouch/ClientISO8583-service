package com.example.js_to_isoservice.iso;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Génère un STAN (champ 11) unique sur 6 chiffres. QMUX associe chaque réponse
 * à sa requête via MTI + STAN : un STAN fixe mélange les requêtes concurrentes.
 */
@Component
public class StanGenerator {

    private final AtomicInteger counter = new AtomicInteger((int) (System.currentTimeMillis() % 999_999));

    public String next() {
        int value = counter.updateAndGet(v -> v >= 999_999 ? 1 : v + 1);
        return String.format("%06d", value);
    }
}
