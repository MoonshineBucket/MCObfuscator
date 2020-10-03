package com.github.dreamsmoke.minecraft.obfuscate.collect;

import com.github.dreamsmoke.minecraft.obfuscate.asm.ClassWrapper;

import java.util.HashMap;
import java.util.Map;

public class ClassCollection {

    protected Map<String, ClassWrapper> classWrappers;
    protected Map<String, byte[]> resources;

    public ClassCollection(Map<String, ClassWrapper> map) {
        classWrappers = new HashMap<>(map);
        resources = new HashMap<>();
    }

    public Map<String, ClassWrapper> getClassWrappers() {
        return classWrappers;
    }

    public Map<String, byte[]> getResources() {
        return resources;
    }

}
