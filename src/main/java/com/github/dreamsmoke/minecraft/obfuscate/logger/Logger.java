package com.github.dreamsmoke.minecraft.obfuscate.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    protected static Logger instance;

    protected File fileRecords;

    protected SimpleDateFormat dateFormat;
    protected String name;

    public Logger() {
        name = "MCObfuscator";
        dateFormat = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");

        try {
            File dataFolder = new File("logs/");
            fileRecords = new File(dataFolder, dateFormat.format(new Date()).concat(".log"));
            if(fileRecords.exists()) fileRecords.delete();
            fileRecords.getParentFile().mkdirs();
            fileRecords.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    }

    public static Logger instance() {
        if(instance == null) instance = new Logger();
        return instance;
    }

    public static void info(String string, Object... objects) {
        log(Level.INFO, string, objects);
    }

    public static void debug(String string, Object... objects) {
        log(Level.DEBUG, string, objects);
    }

    public static void warn(String string, Object... objects) {
        log(Level.WARN, string, objects);
    }

    public static void error(String string, Object... objects) {
        log(Level.ERROR, string, objects);
    }

    public static void log(Level level, String string, Object... objects) {
        log(level, getMessage(string, objects));
    }

    public static void log(Level level, String message) {
        Logger logger = instance();

        try {
            FileWriter fileWriter = new FileWriter(logger.fileRecords, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            String currentTime = logger.dateFormat.format(new Date());
            String string = getMessage("%s [%s] [%s] %s", currentTime,
                    logger.name, level.name, message);

            System.out.println(string);
            bufferedWriter.append(string);

            bufferedWriter.newLine();
            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getMessage(String string, Object... objects) {
        string = String.format(string, objects);
        return string;
    }

    public enum Level {
        INFO("INFO"), DEBUG("DEBUG"), WARN("WARNING"), ERROR("ERROR");

        protected String name;

        Level(String string) {
            name = string;
        }

        public String getName() {
            return name;
        }
    }

}
