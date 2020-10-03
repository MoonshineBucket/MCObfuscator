package com.github.dreamsmoke.minecraft.obfuscate.api;

import com.github.dreamsmoke.minecraft.obfuscate.asm.ClassTree;
import com.github.dreamsmoke.minecraft.obfuscate.asm.ClassWrapper;
import com.github.dreamsmoke.minecraft.obfuscate.logger.Logger;
import com.github.dreamsmoke.minecraft.obfuscate.managers.FileManager;
import com.github.dreamsmoke.minecraft.obfuscate.settings.MCSettings;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mapping {

    protected FileManager fileManager;
    protected MCSettings settings;

    protected File mappingsFile;
    protected Map<String, String> mappings;

    protected Map<String, ClassWrapper> classWrappers;
    protected Map<String, ClassTree> classTrees;

    public Mapping() {
        fileManager = FileManager.instance();
        settings = MCSettings.instance();

        mappings = new HashMap<>();

        classTrees = new HashMap<>();
        classWrappers = new HashMap<>();

        init();
    }

    public Map<String, String> getMappings() {
        return mappings;
    }

    public Map<String, ClassWrapper> getClassWrappers() {
        return classWrappers;
    }

    public Map<String, ClassTree> getClassTrees() {
        return classTrees;
    }

    public ClassTree getTree(String ref) {
        if(!classTrees.containsKey(ref)) {
            ClassWrapper wrapper = classWrappers.get(ref);
            fileManager.readClasses(wrapper, null);
        }

        return classTrees.get(ref);
    }

    public void init() {
        try {
            File dataFolder = new File("mappings/");
            mappingsFile = new File(dataFolder, "mappings.csv");
            if(mappingsFile.exists()) initFile(mappings, mappingsFile);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void initFile(Map<String, String> map, File file) throws IOException {
        List<String> stringList = FileUtils.readLines(file);
        for(String string : stringList) {
            String[] strings = string.split(settings.getPointer());
            map.put(strings[0], strings[1]);
        }
    }

    public void dump() throws IOException {
        if(mappingsFile.exists()) return;
        else {
            mappingsFile.getParentFile().mkdirs();
            mappingsFile.createNewFile();
        }

        Logger.info("Загружаем сгенерированный маппинг в файл!");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(mappingsFile));
        mappings.forEach((originalName, mapping) -> {
            try {
                bufferedWriter.append(originalName).append(settings.getPointer()).append(mapping).append("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        bufferedWriter.close();
        Logger.info("Маппинг успешно перенесен в %s!", mappingsFile.getName());
    }

}
