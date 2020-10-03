package com.github.dreamsmoke.minecraft.obfuscate.settings;

import com.github.dreamsmoke.minecraft.obfuscate.logger.Logger;

import java.io.File;

public class MCSettings {

    protected static MCSettings instance;

    protected String comma, pointer;

    protected File input, output;
    protected String[] obfuscatingPackage, exclusionPackage;
    protected boolean debug;

    public MCSettings() {
        comma = ", ";
        pointer = " -> ";

        input = new File("input.jar");
        output = new File("output.jar");

        obfuscatingPackage = new String[] {"net/minecraft", "com/github/dreamsmoke/minecraft"};
        exclusionPackage = new String[] {"net/minecraftforge"};

        debug = false;
    }

    public static MCSettings instance() {
        if(instance == null) instance = new MCSettings();
        return instance;
    }

    public String getComma() {
        return comma;
    }

    public String getPointer() {
        return pointer;
    }

    public File getInput() {
        return input;
    }

    public File getOutput() {
        return output;
    }

    public String[] getObfuscatingPackage() {
        return obfuscatingPackage;
    }

    public String[] getExclusionPackage() {
        return exclusionPackage;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isObfuscated(String string) {
        for(int i = 0; i < obfuscatingPackage.length; ++i) {
            String[] strings = obfuscatingPackage[i].split(pointer);
            if(string.startsWith(strings[0])) return true;
        }

        return false;
    }

    public boolean isExclude(String string) {
        for(int i = 0; i < exclusionPackage.length; ++i) {
            if(string.startsWith(exclusionPackage[i])) return true;
        }

        return false;
    }

    public String getObfuscated(String string) {
        for(int i = 0; i < obfuscatingPackage.length; ++i) {
            String[] strings = obfuscatingPackage[i].split(pointer);
            if(string.startsWith(strings[0])) {
                if(strings.length > 1) {
                    Logger.info("Используем обнаруженный в параметрах пакет %s для %s!", strings[1], string);
                    return strings[1].endsWith("/") ? strings[1] : strings[1].concat("/");
                } else {
                    Logger.warn("Параметр не был найден, используемый целевой пакет %s!", strings[0]);
                    return strings[0].endsWith("/") ? strings[0] : strings[0].concat("/");
                }
            }
        }

        Logger.warn("Пакет переименования не был найден, используем стандартный %s!", string);
        return string.endsWith("/") ? string : string.concat("/");
    }

    public void init(String[] strings) {
        for(String string : strings) {
            Logger.info("Обнаружен аргумент: %s", string);
            if(string.startsWith("--input=")) input = new File(string.substring("--input=".length()));
            else if(string.startsWith("--output=")) output = new File(string.substring("--output=".length()));
            else if(string.startsWith("--obfuscating=")) {
                String[] obfuscating = string.substring("--obfuscating=".length()).split(comma);
                if(obfuscating.length > 0) obfuscatingPackage = obfuscating;
                else Logger.warn("Аргумент, отвечающий за обфусцируемые пакеты не имеет параметров!");
            } else if(string.startsWith("--exclusion=")) {
                String[] exclusion = string.substring("--exclusion=".length()).split(comma);
                if(exclusion.length > 0) exclusionPackage = exclusion;
                else Logger.warn("Аргумент, отвечающий за исключенные пакеты не имеет параметров!");
            } else if(string.startsWith("--debug=")) debug = string.substring("--debug=".length()).equals("true");
        }

        Logger.info("Настройки программы загружены:");
        Logger.info("Исходный файл: %s", input.getAbsolutePath().replace("\\", "/"));
        Logger.info("Конечный файл: %s", output.getAbsolutePath().replace("\\", "/"));
        Logger.info("Обфусцируемые пакеты: %s", obfuscatingPackage);
        Logger.info("Исключенные пакеты: %s", exclusionPackage);
        Logger.info("Работа дебага: %s", debug);
    }

}
