package com.skyblockexp.ezlifesteal.service;

public sealed interface HeartRecipeSpec permits ShapedHeartRecipeSpec, ShapelessHeartRecipeSpec {

    String heartId();

    int amount();

    String type();
}
