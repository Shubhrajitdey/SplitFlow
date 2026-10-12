package com.splitflow.domain.service;

import java.util.List;
import java.util.Random;

public class FunkyNameGenerator {

    private static final List<String> FUNKY_NAMES = List.of(
            "🍕 Cheesy-Blast",
            "🌮 Taco-Chaos",
            "☕ Caffeine-Fix",
            "🍔 Burger-Binge",
            "🍣 Sushi-Spree",
            "🚕 Turbo-Cab",
            "🍿 Popcorn-Mayhem",
            "🎳 Strike-Storm",
            "🍹 Mocktail-Mood",
            "🍪 Cookie-Crumble",
            "🍩 Donut-Dash",
            "🍦 Gelato-Groove",
            "🍜 Ramen-Rumble",
            "🥑 Guac-Attack",
            "🥞 Pancake-Panic",
            "🥟 Dimsum-Drift"
    );

    private final Random random = new Random();

    public String generate() {
        return FUNKY_NAMES.get(random.nextInt(FUNKY_NAMES.size()));
    }
}
