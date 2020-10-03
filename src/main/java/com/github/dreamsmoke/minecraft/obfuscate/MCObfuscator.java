package com.github.dreamsmoke.minecraft.obfuscate;

import com.github.dreamsmoke.minecraft.obfuscate.logger.Logger;
import com.github.dreamsmoke.minecraft.obfuscate.settings.MCSettings;
import com.github.dreamsmoke.minecraft.obfuscate.transformers.RenameTransformer;
import com.github.dreamsmoke.minecraft.obfuscate.transformers.abstracts.Transformer;

public class MCObfuscator {

    protected MCSettings settings;

    public static void main(String[] args) {
        Logger.info("Программа запущена, загружаем настройки!");
        new MCObfuscator().init(args);
    }

    public MCSettings getSettings() {
        return settings;
    }

    public void init(String[] args) {
        (settings = MCSettings.instance()).init(args);
        Logger.info("Запускаем процесс ремаппинга!");
        Transformer transformer = new RenameTransformer(this);
        transformer.transform();
    }

}
