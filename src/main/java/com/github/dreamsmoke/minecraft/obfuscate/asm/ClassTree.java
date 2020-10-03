package com.github.dreamsmoke.minecraft.obfuscate.asm;

import java.util.HashSet;
import java.util.Set;

public class ClassTree {

    public ClassWrapper classWrapper;

    public Set<String> parentClasses;
    public Set<String> subClasses;

    public ClassTree(ClassWrapper wrapper) {
        classWrapper = wrapper;

        parentClasses = new HashSet<>();
        subClasses = new HashSet<>();
    }

}
