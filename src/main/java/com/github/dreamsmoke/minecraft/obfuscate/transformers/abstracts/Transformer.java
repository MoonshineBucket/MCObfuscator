package com.github.dreamsmoke.minecraft.obfuscate.transformers.abstracts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Transformer {

    protected List<String> memoryMappings;
    protected char[] letters, numbers;

    public Transformer() {
        memoryMappings = new ArrayList<>();

        letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
        numbers = "0123456789".toCharArray();
    }

    public abstract void transform();
    public abstract String getName();

    public String generateMapping(char[] chars, int length) {
        String string;

        do {
            string = generateString(chars, length);
        } while (memoryMappings.contains(string));
        memoryMappings.add(string);
        return string;
    }

    public String generateString(char[] chars, int length) {
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < length; ++i) {
            stringBuilder.append(chars[getRandom(chars.length)]);
        }

        return stringBuilder.toString();
    }

    public int getRandom(int bounds) {
        return ThreadLocalRandom.current().nextInt(bounds);
    }

}
