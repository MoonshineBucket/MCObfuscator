package com.github.dreamsmoke.minecraft.obfuscate.asm;

import org.objectweb.asm.tree.MethodNode;

public class MethodWrapper {

    public MethodNode methodNode;
    public ClassWrapper owner;

    public String originalName;
    public String originalDescription;

    public MethodWrapper(MethodNode node, ClassWrapper classWrapper, String name, String description) {
        methodNode = node;
        owner = classWrapper;

        originalName = name;
        originalDescription = description;
    }

}
