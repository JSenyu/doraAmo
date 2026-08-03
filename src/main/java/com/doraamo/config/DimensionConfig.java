package com.doraamo.config;

import com.doraamo.DoraAmo;
import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.util.LangKeys;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DimensionConfig {

    public static final class DimensionEntry {
        public final int id;
        /** Lang key used for display, e.g. doraamo.dimension.0 */
        public final String langKey;

        public DimensionEntry(int id, String langKey) {
            this.id = id;
            this.langKey = langKey;
        }
    }

    private static Configuration config;
    private static List<DimensionEntry> dimensions = new ArrayList<DimensionEntry>();

    private DimensionConfig() {
    }

    public static void init(File file) {
        config = new Configuration(file);
        load();
    }

    public static void load() {
        if (config == null) {
            return;
        }
        config.load();

        String[] defaults = new String[] {
                "0",
                "-1",
                "1"
        };

        String[] lines = config.getStringList(
                "dimensions",
                Configuration.CATEGORY_GENERAL,
                defaults,
                "Preferred dimension order for Anywhere Door. Format: id  or  id|lang.key. "
                        + "Registered mod dimensions not listed here still appear in the Coordinator GUI."
        );

        List<DimensionEntry> parsed = new ArrayList<DimensionEntry>();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\|", 3);
            try {
                int id = Integer.parseInt(parts[0].trim());
                String langKey;
                if (parts.length == 1 || parts[1].trim().isEmpty()) {
                    langKey = LangKeys.dimensionKey(id);
                } else if (parts.length == 2) {
                    langKey = parts[1].trim();
                } else {
                    langKey = LangKeys.dimensionKey(id);
                }
                parsed.add(new DimensionEntry(id, langKey));
            } catch (NumberFormatException e) {
                DoraAmo.logger.warn("Invalid dimension id in config entry: {}", line);
            }
        }

        if (parsed.isEmpty()) {
            parsed.add(new DimensionEntry(0, LangKeys.dimensionKey(0)));
            parsed.add(new DimensionEntry(-1, LangKeys.dimensionKey(-1)));
            parsed.add(new DimensionEntry(1, LangKeys.dimensionKey(1)));
        }

        dimensions = Collections.unmodifiableList(parsed);
        config.save();
    }

    public static List<DimensionEntry> getDimensions() {
        return dimensions;
    }

    /**
     * Config order first, then any other dimensions registered with Forge (mod dims).
     */
    public static List<DimensionEntry> getGuiDimensions() {
        Map<Integer, DimensionEntry> ordered = new LinkedHashMap<Integer, DimensionEntry>();
        for (DimensionEntry entry : dimensions) {
            ordered.put(Integer.valueOf(entry.id), entry);
        }
        try {
            Integer[] registered = DimensionManager.getStaticDimensionIDs();
            if (registered != null) {
                for (Integer idObj : registered) {
                    if (idObj == null) {
                        continue;
                    }
                    int id = idObj.intValue();
                    if (!ordered.containsKey(Integer.valueOf(id))) {
                        ordered.put(Integer.valueOf(id), new DimensionEntry(id, LangKeys.dimensionKey(id)));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return new ArrayList<DimensionEntry>(ordered.values());
    }

    public static DimensionEntry findById(int id) {
        for (DimensionEntry entry : getGuiDimensions()) {
            if (entry.id == id) {
                return entry;
            }
        }
        return null;
    }

    public static ITextComponent getDisplayComponent(int dimensionId) {
        if (dimensionId == DoraAmo.BLANK_DIMENSION) {
            return new TextComponentTranslation(LangKeys.DIMENSION_BLANK);
        }
        return new TextComponentString(DisplayCatalog.displayDimension(dimensionId));
    }

    public static String getLangKey(int dimensionId) {
        if (dimensionId == DoraAmo.BLANK_DIMENSION) {
            return LangKeys.DIMENSION_BLANK;
        }
        DimensionEntry entry = findById(dimensionId);
        if (entry != null) {
            return entry.langKey;
        }
        return LangKeys.DIMENSION_UNKNOWN;
    }

    /**
     * Cycle: blank -> config dims in order -> blank.
     */
    public static int nextDimensionId(int currentId) {
        List<DimensionEntry> list = dimensions;
        if (list.isEmpty()) {
            return DoraAmo.BLANK_DIMENSION;
        }
        if (currentId == DoraAmo.BLANK_DIMENSION) {
            return list.get(0).id;
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == currentId) {
                if (i + 1 < list.size()) {
                    return list.get(i + 1).id;
                }
                return DoraAmo.BLANK_DIMENSION;
            }
        }
        return list.get(0).id;
    }
}
