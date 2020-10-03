package com.github.dreamsmoke.minecraft.obfuscate.asm;

import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

public class ClassWrapper {

    public ClassNode classNode;

    public String originalName;

    public boolean libraryNode;

    public List<MethodWrapper> methods;
    public List<FieldWrapper> fields;

    public ClassWrapper(ClassNode node, boolean library) {
        classNode = node;
        originalName = classNode.name;
        libraryNode = library;

        methods = new ArrayList<>();
        fields = new ArrayList<>();

        ClassWrapper instance = this;
        classNode.methods.forEach(methodNode -> methods
                .add(new MethodWrapper(methodNode, instance, methodNode.name, methodNode.desc)));
        if(classNode.fields != null) classNode.fields.forEach(fieldNode -> fields.add(new FieldWrapper(fieldNode,
                instance, fieldNode.name, fieldNode.desc)));
    }

}
