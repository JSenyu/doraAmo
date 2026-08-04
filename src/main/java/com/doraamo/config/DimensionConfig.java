package com.doraamo.config;

import com.doraamo.DoraAmo;
import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.util.DimUtil;
import com.doraamo.util.LangKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DimensionConfig {

    public static final class DimensionEntry {
        public final String dimKey;
        public final String langKey;

        public DimensionEntry(String dimKey, String langKey) {
            this.dimKey = DimUtil.normalize(dimKey);
            this.langKey = langKey;
        }
    }

    private static File configFile;
    private static List<DimensionEntry> dimensions = new ArrayList<>();

    private DimensionConfig() {
    }

    public static void init(File file) {
        configFile = file;
        load();
    }

    public static void load() {
        if (configFile == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        if (configFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    lines.add(line);
                }
            } catch (Exception e) {
                DoraAmo.logger.warn("Failed to read dimension config: {}", e.toString());
            }
        }
        if (lines.isEmpty()) {
            lines.add(DimUtil.OVERWORLD);
            lines.add(DimUtil.NETHER);
            lines.add(DimUtil.END);
            saveLines(lines);
        }
        parseLines(lines);
    }

    private static void saveLines(List<String> lines) {
        if (configFile == null) {
            return;
        }
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            writer.write("# Preferred dimension order. Format: namespace:path  or  namespace:path|lang.key\n");
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (Exception e) {
            DoraAmo.logger.warn("Failed to write dimension config: {}", e.toString());
        }
    }

    private static void parseLines(List<String> lines) {
        List<DimensionEntry> parsed = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\|", 2);
            String dimKey = migrateDimToken(parts[0].trim());
            String langKey = parts.length > 1 && !parts[1].trim().isEmpty()
                    ? parts[1].trim()
                    : LangKeys.dimensionKey(dimKey);
            parsed.add(new DimensionEntry(dimKey, langKey));
        }
        if (parsed.isEmpty()) {
            parsed.add(new DimensionEntry(DimUtil.OVERWORLD, LangKeys.dimensionKey(DimUtil.OVERWORLD)));
            parsed.add(new DimensionEntry(DimUtil.NETHER, LangKeys.dimensionKey(DimUtil.NETHER)));
            parsed.add(new DimensionEntry(DimUtil.END, LangKeys.dimensionKey(DimUtil.END)));
        }
        dimensions = Collections.unmodifiableList(parsed);
    }

    private static String migrateDimToken(String token) {
        try {
            return DimUtil.fromLegacyInt(Integer.parseInt(token));
        } catch (NumberFormatException e) {
            return DimUtil.normalize(token);
        }
    }

    public static List<DimensionEntry> getDimensions() {
        return dimensions;
    }

    public static List<DimensionEntry> getGuiDimensions(MinecraftServer server) {
        Map<String, DimensionEntry> ordered = new LinkedHashMap<>();
        for (DimensionEntry entry : dimensions) {
            ordered.put(entry.dimKey, entry);
        }
        if (server != null) {
            for (String key : DimUtil.allLevelKeys(server)) {
                ordered.putIfAbsent(key, new DimensionEntry(key, LangKeys.dimensionKey(key)));
            }
        }
        return new ArrayList<>(ordered.values());
    }

    public static List<DimensionEntry> getGuiDimensions() {
        return getGuiDimensions(null);
    }

    public static DimensionEntry findByKey(String dimKey) {
        String norm = DimUtil.normalize(dimKey);
        for (DimensionEntry entry : getGuiDimensions()) {
            if (entry.dimKey.equals(norm)) {
                return entry;
            }
        }
        return null;
    }

    public static Component getDisplayComponent(String dimensionKey) {
        if (DimUtil.isBlank(dimensionKey)) {
            return Component.translatable(LangKeys.DIMENSION_BLANK);
        }
        return Component.literal(DisplayCatalog.displayDimension(dimensionKey));
    }

    public static String getLangKey(String dimensionKey) {
        if (DimUtil.isBlank(dimensionKey)) {
            return LangKeys.DIMENSION_BLANK;
        }
        DimensionEntry entry = findByKey(dimensionKey);
        return entry != null ? entry.langKey : LangKeys.DIMENSION_UNKNOWN;
    }

    public static String nextDimension(String currentKey) {
        List<DimensionEntry> list = dimensions;
        if (list.isEmpty()) {
            return DoraAmo.BLANK_DIMENSION;
        }
        if (DimUtil.isBlank(currentKey)) {
            return list.get(0).dimKey;
        }
        String norm = DimUtil.normalize(currentKey);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).dimKey.equals(norm)) {
                if (i + 1 < list.size()) {
                    return list.get(i + 1).dimKey;
                }
                return DoraAmo.BLANK_DIMENSION;
            }
        }
        return list.get(0).dimKey;
    }
}
