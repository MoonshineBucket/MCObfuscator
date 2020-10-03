package com.github.dreamsmoke.minecraft.obfuscate.asm;

import org.objectweb.asm.tree.FieldNode;

public class FieldWrapper {

    public FieldNode fieldNode;
    public ClassWrapper owner;

    public String originalName;
    public String originalDescription;

    public FieldWrapper(FieldNode node, ClassWrapper classWrapper, String name, String description) {
        fieldNode = node;
        owner = classWrapper;

        originalName = name;
        originalDescription = description;
    }

}
