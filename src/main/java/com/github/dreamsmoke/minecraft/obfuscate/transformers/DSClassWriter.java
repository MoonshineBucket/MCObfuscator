package com.github.dreamsmoke.minecraft.obfuscate.transformers;

import com.github.dreamsmoke.minecraft.obfuscate.api.Mapping;
import com.github.dreamsmoke.minecraft.obfuscate.asm.ClassTree;
import com.github.dreamsmoke.minecraft.obfuscate.asm.ClassWrapper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class DSClassWriter extends ClassWriter {

    protected Mapping mapping;

    public DSClassWriter(int flags, Mapping mcMapping) {
        super(flags);

        mapping = mcMapping;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if("java/lang/Object".equals(type1) || "java/lang/Object".equals(type2)) return "java/lang/Object";

        String first = deriveCommonSuperName(type1, type2);
        String second = deriveCommonSuperName(type2, type1);

        if(!"java/lang/Object".equals(first)) return first;
        if(!"java/lang/Object".equals(second)) return second;

        return getCommonSuperClass(returnClazz(type1).superName, returnClazz(type2).superName);
    }

    private String deriveCommonSuperName(String type1, String type2) {
        ClassNode first = returnClazz(type1);
        ClassNode second = returnClazz(type2);

        if(isAssignableFrom(type1, type2)) return type1;
        else if(isAssignableFrom(type2, type1)) return type2;
        else if(Modifier.isInterface(first.access) || Modifier.isInterface(second.access))
            return "java/lang/Object";
        else {
            do {
                type1 = first.superName;
                first = returnClazz(type1);
            } while (!isAssignableFrom(type1, type2));
            return type1;
        }
    }

    private ClassNode returnClazz(String ref) {
        ClassWrapper clazz = mapping.getClassWrappers().get(ref);
        if(clazz == null) throw new RuntimeException(ref + " Должен находиться в списке зависимостей!");
        return clazz.classNode;
    }

    private boolean isAssignableFrom(String type1, String type2) {
        if("java/lang/Object".equals(type1)) return true;
        if(type1.equals(type2)) return true;

        returnClazz(type1);
        returnClazz(type2);

        ClassTree firstTree = mapping.getTree(type1);
        if(firstTree == null) throw new RuntimeException("Ошибка, " + type1
                + " не обнаружен в списке зависимостей!");
        Set<String> allChildren = new HashSet<>();
        Deque<String> toProcess = new ArrayDeque<>(firstTree.subClasses);
        while(!toProcess.isEmpty()) {
            String s = toProcess.poll();
            if(allChildren.add(s)) {
                returnClazz(s);
                ClassTree tempTree = mapping.getTree(s);
                toProcess.addAll(tempTree.subClasses);
            }
        }

        return allChildren.contains(type2);
    }

}
