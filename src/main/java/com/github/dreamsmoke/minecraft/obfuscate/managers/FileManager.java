package com.github.dreamsmoke.minecraft.obfuscate.managers;

import com.github.dreamsmoke.minecraft.obfuscate.api.Mapping;
import com.github.dreamsmoke.minecraft.obfuscate.asm.ClassTree;
import com.github.dreamsmoke.minecraft.obfuscate.asm.ClassWrapper;
import com.github.dreamsmoke.minecraft.obfuscate.collect.ClassCollection;
import com.github.dreamsmoke.minecraft.obfuscate.logger.Logger;
import com.github.dreamsmoke.minecraft.obfuscate.settings.MCSettings;
import com.github.dreamsmoke.minecraft.obfuscate.transformers.DSClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileManager {

    protected static FileManager instance;
    protected MCSettings settings;

    protected Mapping mapping;

    public FileManager() {
        settings = MCSettings.instance();
    }

    public static FileManager instance() {
        if(instance == null) instance = new FileManager();
        return instance;
    }

    public void setMapping(Mapping mcMapping) {
        mapping = mcMapping;
    }

    public ClassCollection readFile(Map<String, ClassWrapper> classPatch, File file) {
        if(file.exists()) {
            Map<String, ClassWrapper> classWrappers = new HashMap<>();
            Map<String, byte[]> resources = new HashMap<>();

            try {
                ZipFile zipFile = new ZipFile(file);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if(entry.isDirectory()) continue;

                    String entryName = entry.getName();
                    if(entryName.endsWith(".class")) {
                        try {
                            ClassReader classReader = new ClassReader(zipFile.getInputStream(entry));
                            ClassNode classNode = new ClassNode();

                            classReader.accept(classNode, ClassReader.SKIP_FRAMES);
                            if(classNode.version <= Opcodes.V1_5) {
                                Logger.warn("Обнаружена устаревшая версия asm-%s в классе %s",
                                        entryName, classNode.version);
                                for(int i = 0; i < classNode.methods.size(); i++) {
                                    MethodNode methodNode = classNode.methods.get(i);
                                    JSRInlinerAdapter adapter = new JSRInlinerAdapter(methodNode, methodNode.access,
                                            methodNode.name, methodNode.desc, methodNode.signature,
                                            methodNode.exceptions.toArray(new String[0]));
                                    methodNode.accept(adapter);
                                    classNode.methods.set(i, adapter);
                                }
                            }

                            ClassWrapper classWrapper = new ClassWrapper(classNode, false);
                            classWrappers.put(classWrapper.originalName, classWrapper);
                        } catch (Throwable throwable) {
                            resources.put(entry.getName(), toByteArray(zipFile.getInputStream(entry)));
                        }
                    } else {
                        resources.put(entry.getName(), toByteArray(zipFile.getInputStream(entry)));
                    }

                    if(settings.isDebug()) Logger.debug("%s %s успешно загружен!", (entryName
                            .endsWith(".class") ? "Класс" : "Ресурс"), entryName);
                }
            } catch (ZipException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ClassCollection collection = new ClassCollection(classWrappers);
            collection.getResources().putAll(resources);
            classPatch.putAll(classWrappers);
            return collection;
        } else {
            // I`m not understand...
        }

        return null;
    }

    public void writeFile(ClassCollection collection, File file) throws IOException {
        if(file.exists()) file.delete();
        file.createNewFile();

        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
            collection.getClassWrappers().values().forEach(classWrapper -> {
                try {
                    ZipEntry entry = new ZipEntry(classWrapper.classNode.name + ".class");
                    entry.setLastModifiedTime(FileTime.fromMillis(0));
                    entry.setCompressedSize(-1);

                    ClassWriter cw = new DSClassWriter(ClassWriter.COMPUTE_FRAMES, mapping);
                    cw.newUTF8("MCObfuscator1.0");

                    try {
                        classWrapper.classNode.accept(cw);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        Logger.warn("Произошла ошибка при записи класса %s, используем стандартный вариант!",
                                entry.getName());

                        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        cw.newUTF8("MCObfuscator1.0");
                        classWrapper.classNode.accept(cw);
                    }

                    zos.putNextEntry(entry);
                    zos.write(cw.toByteArray());
                    zos.closeEntry();

                    if(settings.isDebug()) Logger.debug("Класс %s успешно загружен!", entry.getName());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });

            collection.getResources().forEach((name, bytes) -> {
                try {
                    ZipEntry entry = new ZipEntry(name);
                    entry.setLastModifiedTime(FileTime.fromMillis(0));
                    entry.setCompressedSize(-1);

                    zos.putNextEntry(entry);
                    zos.write(bytes);
                    zos.closeEntry();

                    if(settings.isDebug()) Logger.debug("Ресурс %s успешно загружен!", entry.getName());
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            });

            zos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void readClasses(File dataFolder) {
        if(dataFolder.exists()) Logger.info("Обнаружено %s файлов зависемостей!",
                dataFolder.listFiles().length);
        else dataFolder.mkdir();

        for(File file : dataFolder.listFiles()) {
            if(file.exists()) {
                if(file.isDirectory()) readClasses(file);
                else {
                    Logger.info("Загружаем классы зависимости: %s", file.getName());
                    readClasses(mapping.getClassWrappers(), file);
                }
            }
        }
    }

    public void readClasses(ClassWrapper classWrapper, ClassWrapper sub) {
        if(mapping.getClassTrees().get(classWrapper.classNode.name) == null) {
            ClassTree tree = new ClassTree(classWrapper);
            if(classWrapper.classNode.superName != null) {
                tree.parentClasses.add(classWrapper.classNode.superName);
                ClassWrapper superClass = mapping.getClassWrappers().get(classWrapper.classNode.superName);
                if(superClass == null) throw new RuntimeException(classWrapper.classNode
                        .superName + " Должен находиться в списке зависимостей!");
                readClasses(superClass, classWrapper);
            }

            if(classWrapper.classNode.interfaces != null && !classWrapper.classNode.interfaces.isEmpty()) {
                for(String string : classWrapper.classNode.interfaces) {
                    tree.parentClasses.add(string);
                    ClassWrapper interfaceClass = mapping.getClassWrappers().get(string);
                    if(interfaceClass == null) throw new RuntimeException(string +
                            " Должен находиться в списке зависимостей!");
                    readClasses(interfaceClass, classWrapper);
                }
            }

            mapping.getClassTrees().put(classWrapper.classNode.name, tree);
        }

        if(sub != null) mapping.getClassTrees().get(classWrapper.classNode.name).subClasses.add(sub.classNode.name);
    }

    public void readClasses(Map<String, ClassWrapper> classPath, File file) {
        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if(entry.isDirectory()) continue;

                String entryName = entry.getName();
                if(entryName.endsWith(".class")) {
                    try {
                        ClassReader cr = new ClassReader(zipFile.getInputStream(entry));
                        ClassNode classNode = new ClassNode();

                        cr.accept(classNode, ClassReader.SKIP_CODE
                                | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                        ClassWrapper classWrapper = new ClassWrapper(classNode, true);
                        classPath.put(classWrapper.originalName, classWrapper);

                        if(settings.isDebug()) Logger.debug("Класс %s успешно загружен в список зависимостей!",
                                entryName);
                    } catch (Throwable t) {
                        // Don't care.
                    }
                }
            }
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isNative(int access) {
        return (Opcodes.ACC_NATIVE & access) != 0;
    }

    public byte[] toByteArray(InputStream in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while(in.available() > 0) {
                int data = in.read(buffer);
                out.write(buffer, 0, data);
            }

            in.close();
            out.close();
            return out.toByteArray();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

}
