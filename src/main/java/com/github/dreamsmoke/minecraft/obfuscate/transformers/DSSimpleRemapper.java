package com.github.dreamsmoke.minecraft.obfuscate.transformers;

import org.objectweb.asm.commons.SimpleRemapper;

import java.util.Map;

public class DSSimpleRemapper extends SimpleRemapper {

    public DSSimpleRemapper(Map<String, String> mapping) {
        super(mapping);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        String remappedName = map(owner + '.' + name + '.' + desc);
        return (remappedName != null) ? remappedName : name;
    }

}
