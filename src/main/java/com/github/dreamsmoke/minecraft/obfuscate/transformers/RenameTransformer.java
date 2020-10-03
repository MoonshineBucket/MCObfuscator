package com.github.dreamsmoke.minecraft.obfuscate.transformers;

import com.github.dreamsmoke.minecraft.obfuscate.MCObfuscator;
import com.github.dreamsmoke.minecraft.obfuscate.api.Mapping;
import com.github.dreamsmoke.minecraft.obfuscate.asm.ClassTree;
import com.github.dreamsmoke.minecraft.obfuscate.asm.ClassWrapper;
import com.github.dreamsmoke.minecraft.obfuscate.asm.FieldWrapper;
import com.github.dreamsmoke.minecraft.obfuscate.asm.MethodWrapper;
import com.github.dreamsmoke.minecraft.obfuscate.collect.ClassCollection;
import com.github.dreamsmoke.minecraft.obfuscate.logger.Logger;
import com.github.dreamsmoke.minecraft.obfuscate.managers.FileManager;
import com.github.dreamsmoke.minecraft.obfuscate.settings.MCSettings;
import com.github.dreamsmoke.minecraft.obfuscate.transformers.abstracts.Transformer;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RenameTransformer extends Transformer {

    protected MCObfuscator obfuscator;
    protected MCSettings settings;

    protected FileManager fileManager;

    protected Mapping mapping;

    public RenameTransformer(MCObfuscator instance) {
        super();

        fileManager = FileManager.instance();

        obfuscator = instance;
        settings = obfuscator.getSettings();

        fileManager.setMapping(mapping = new Mapping());
    }

    @Override
    public void transform() {
        try {
            Logger.info("Загружаем зависимости программы!");
            fileManager.readClasses(new File("libraries"));
            Logger.info("Зависимости успешно загружены, приступаем к загрузке исходного файла!");
            if(settings.isDebug()) Logger.debug("Было загружено %s классов зависимостей!",
                    mapping.getClassWrappers().size());

            if(settings.isDebug()) Logger.debug("Загружаем исходный файл %s",
                    settings.getInput().getAbsolutePath());

            ClassCollection collection = fileManager.readFile(mapping.getClassWrappers(), settings.getInput());
            Logger.info("Исходный файл успешно загружен, приступаем к маппингам!");

            collection.getClassWrappers().values().forEach(classWrapper -> fileManager
                    .readClasses(classWrapper, null));

            if(mapping.getMappings().size() == 0) {
                for(ClassWrapper classWrapper : collection.getClassWrappers().values()) {
                    String originalName = classWrapper.originalName;
                    if(settings.isDebug()) Logger.debug("Работаем с классом %s", originalName);

                    if(settings.isExclude(originalName)) {
                        if(settings.isDebug()) Logger.debug("Пропускаем класс-исключение %s", originalName);
                        continue;
                    }

                    classWrapper.methods.stream().filter(methodWrapper -> !fileManager
                            .isNative(methodWrapper.methodNode.access) && !"main".equals(methodWrapper.methodNode.name)
                            && !"premain".equals(methodWrapper.methodNode.name) && !methodWrapper.methodNode
                            .name.startsWith("<")).forEach(methodWrapper -> {
                        if(canRenameMethodTree(new HashSet<>(), methodWrapper, originalName)) {
                            String string = generateName(8);

                            renameMethodTree(new HashSet<>(), methodWrapper, originalName, string);
                            if(settings.isDebug()) Logger.debug("Сгенерировали новое имя для метода %s %s %s",
                                    methodWrapper.originalName, settings.getPointer(), string);
                        }
                    });

                    classWrapper.fields.forEach(fieldWrapper -> {
                        if(canRenameFieldTree(new HashSet<>(), fieldWrapper, originalName)) {
                            String string = generateName(8);

                            renameFieldTree(new HashSet<>(), fieldWrapper, originalName, string);
                            if(settings.isDebug()) Logger.debug("Сгенерировали новое имя для переменной %s %s %s",
                                    fieldWrapper.originalName, settings.getPointer(), string);
                        }
                    });

                    if(settings.isObfuscated(originalName) && !mapping.getMappings().containsKey(originalName)) {
                        String string = generateName(8);

                        mapping.getMappings().put(originalName, settings.getObfuscated(originalName) + string);
                        if(settings.isDebug()) Logger.debug("Сгенерировали новое имя для класса %s %s %s",
                                originalName, settings.getPointer(), string);
                    }
                }

                Logger.info("Маппинги успешно сгенерированы, начинаем выполнять замену!");
            } else Logger.info("Используем заранее сгенерированные маппинги!");

            Logger.info("Начинаем работу с изменением классов!");
            Remapper simpleRemapper = new DSSimpleRemapper(mapping.getMappings());
            for(ClassWrapper classWrapper : new ArrayList<>(collection.getClassWrappers().values())) {
                if(settings.isDebug()) Logger.debug("Работаем с классом " + classWrapper.originalName);
                if(settings.isExclude(classWrapper.originalName)) {
                    if(settings.isDebug()) Logger.debug("Пропускаем класс-исключение %s",
                            classWrapper.originalName);
                    continue;
                }

                ClassNode classNode = classWrapper.classNode;

                ClassNode copy = new ClassNode();
                classNode.accept(new ClassRemapper(copy, simpleRemapper));

                List<String> strings = new ArrayList<>();
                for(int i = 0; i < copy.methods.size(); i++) {
                    strings.add(classWrapper.methods.get(i).originalName);
                    classWrapper.methods.get(i).methodNode = copy.methods.get(i);
                }

                if(settings.isDebug()) Logger.debug("Список методов: " + strings.toString());

                strings = new ArrayList<>();
                if(copy.fields != null) {
                    for(int i = 0; i < copy.fields.size(); i++) {
                        strings.add(classWrapper.fields.get(i).originalName);
                        classWrapper.fields.get(i).fieldNode = copy.fields.get(i);
                    }

                    if(settings.isDebug()) Logger.debug("Список переменных: " + strings.toString());
                }

                classWrapper.classNode = copy;
                collection.getClassWrappers().remove(classWrapper.originalName);
                collection.getClassWrappers().put(classWrapper.classNode.name, classWrapper);
                mapping.getClassWrappers().put(classWrapper.classNode.name, classWrapper);
            }

            Logger.info("Закончили работу с маппингами!");

            mapping.dump();
            Logger.info("Выгружаем отредактированные файлы в конечный продукт!");
            fileManager.writeFile(collection, settings.getOutput());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "RenameTransformer";
    }

    public String generateName(int length) {
        return generateMapping(letters, length) + "_" + generateMapping(numbers, length);
    }

    private boolean canRenameMethodTree(HashSet<ClassTree> visited, MethodWrapper methodWrapper, String owner) {
        ClassTree tree = mapping.getTree(owner);
        if (!visited.contains(tree)) {
            visited.add(tree);

            if(mapping.getMappings().containsKey(owner + '.' + methodWrapper
                    .originalName + methodWrapper.originalDescription)) return false;
            if(!methodWrapper.owner.originalName.equals(owner) && tree.classWrapper.libraryNode)
                for(MethodNode mn : tree.classWrapper.classNode.methods)
                    if(mn.name.equals(methodWrapper.originalName) & mn.desc.equals(methodWrapper
                            .originalDescription)) return false;
            for (String parent : tree.parentClasses)
                if (parent != null && !canRenameMethodTree(visited, methodWrapper, parent))
                    return false;
            for (String sub : tree.subClasses)
                if (sub != null && !canRenameMethodTree(visited, methodWrapper, sub))
                    return false;
        }

        return true;
    }

    private void renameMethodTree(HashSet<ClassTree> visited, MethodWrapper MethodWrapper,
                                  String className, String newName) {
        ClassTree tree = mapping.getTree(className);
        if(!tree.classWrapper.libraryNode && !visited.contains(tree)) {
            mapping.getMappings().put(className + '.' + MethodWrapper.originalName + MethodWrapper
                    .originalDescription, newName);
            visited.add(tree);

            tree.parentClasses.forEach(parentClass -> renameMethodTree(visited, MethodWrapper, parentClass, newName));
            tree.subClasses.forEach(subClass -> renameMethodTree(visited, MethodWrapper, subClass, newName));
        }
    }

    private boolean canRenameFieldTree(HashSet<ClassTree> visited, FieldWrapper fieldWrapper, String owner) {
        ClassTree tree = mapping.getTree(owner);
        if(!visited.contains(tree)) {
            visited.add(tree);

            if(mapping.getMappings().containsKey(owner + '.' + fieldWrapper
                    .originalName + '.' + fieldWrapper.originalDescription)) return false;
            if(!fieldWrapper.owner.originalName.equals(owner) && tree.classWrapper.libraryNode)
                for(FieldNode fn : tree.classWrapper.classNode.fields)
                    if(fieldWrapper.originalName.equals(fn.name) && fieldWrapper.originalDescription
                            .equals(fn.desc)) return false;
            for(String parent : tree.parentClasses)
                if(parent != null && !canRenameFieldTree(visited,
                        fieldWrapper, parent)) return false;
            for(String sub : tree.subClasses)
                if(sub != null && !canRenameFieldTree(visited,
                        fieldWrapper, sub)) return false;
        }

        return true;
    }

    private void renameFieldTree(HashSet<ClassTree> visited, FieldWrapper fieldWrapper, String owner, String newName) {
        ClassTree tree = mapping.getTree(owner);
        if(!tree.classWrapper.libraryNode && !visited.contains(tree)) {
            mapping.getMappings().put(owner + '.' + fieldWrapper.originalName + '.' + fieldWrapper
                    .originalDescription, newName);
            visited.add(tree);

            tree.parentClasses.forEach(parentClass -> renameFieldTree(visited, fieldWrapper, parentClass, newName));
            tree.subClasses.forEach(subClass -> renameFieldTree(visited, fieldWrapper, subClass, newName));
        }
    }

}
