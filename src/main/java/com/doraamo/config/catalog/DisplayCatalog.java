package com.doraamo.config.catalog;

import com.doraamo.DoraAmo;
import com.doraamo.destination.DestinationLocator;
import com.doraamo.util.DimUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auto-generated, user-editable JSON catalogs under {@code config/doraamo/catalog/}.
 * <pre>
 *   dimensions.json  — key: dimension id ("0", "-1", "7")
 *   biomes.json      — key: registry id ("minecraft:plains")
 *   structures.json  — key: structure name ("Village", "mymod:Ruins")
 * </pre>
 * Missing Chinese names fall back to id display. Edit name_zh / name_en / pinyin / initials freely;
 * restarts merge newly registered content without wiping your edits.
 */
public final class DisplayCatalog {

    public enum Kind {
        DIMENSION("dimensions.json"),
        BIOME("biomes.json"),
        STRUCTURE("structures.json");

        public final String fileName;

        Kind(String fileName) {
            this.fileName = fileName;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static File catalogDir;
    private static CatalogFile dimensions = new CatalogFile();
    private static CatalogFile biomes = new CatalogFile();
    private static CatalogFile structures = new CatalogFile();
    private static boolean preferChinese = true;

    private DisplayCatalog() {
    }

    public static void init(File directory) {
        catalogDir = directory;
        if (!catalogDir.exists() && !catalogDir.mkdirs()) {
            DoraAmo.logger.warn("Could not create catalog dir {}", catalogDir);
        }
        writeReadme();
        dimensions = loadOrCreate(Kind.DIMENSION);
        biomes = loadOrCreate(Kind.BIOME);
        structures = loadOrCreate(Kind.STRUCTURE);
        seedDefaultsIfEmpty();
        saveAll();
    }

    /** Call after registries are ready (postInit / loadComplete). */
    public static void syncFromGame() {
        if (catalogDir == null) {
            return;
        }
        boolean changed = false;
        changed |= syncDimensions();
        changed |= syncBiomes();
        changed |= syncStructures();
        if (changed) {
            saveAll();
            DoraAmo.logger.info("Updated DoraAmo catalog under {}", catalogDir.getAbsolutePath());
        }
    }

    public static void setPreferChinese(boolean prefer) {
        preferChinese = prefer;
    }

    public static boolean preferChinese() {
        return preferChinese;
    }

    @Nullable
    public static CatalogEntry get(Kind kind, String id) {
        CatalogFile file = fileFor(kind);
        if (file == null || file.entries == null || id == null) {
            return null;
        }
        return file.entries.get(id);
    }

    public static String displayDimension(String dimKey) {
        if (DimUtil.isBlank(dimKey)) {
            return "?";
        }
        String norm = DimUtil.normalize(dimKey);
        CatalogEntry e = get(Kind.DIMENSION, norm);
        if (e == null) {
            e = get(Kind.DIMENSION, Integer.toString(DimUtil.toLegacyInt(norm)));
        }
        String named = e == null ? null : e.pickName(preferChinese);
        if (named != null) {
            return named;
        }
        return norm;
    }

    public static String displayBiome(Biome biome) {
        if (biome == null) {
            return "?";
        }
        ResourceLocation key = ForgeRegistries.BIOMES.getKey(biome);
        String id = key != null ? key.toString() : "minecraft:plains";
        CatalogEntry e = get(Kind.BIOME, id);
        String named = e == null ? null : e.pickName(preferChinese);
        if (named != null) {
            return named;
        }
        return id;
    }

    public static String displayStructure(String structureId) {
        if (structureId == null || structureId.isEmpty()) {
            return "?";
        }
        CatalogEntry e = get(Kind.STRUCTURE, structureId);
        String named = e == null ? null : e.pickName(preferChinese);
        if (named != null) {
            return named;
        }
        return structureId;
    }

    /** Extra candidates for SearchFilter (pinyin, initials, names, id). */
    public static String[] searchTokens(Kind kind, String id, String... extra) {
        List<String> list = new ArrayList<String>();
        if (id != null) {
            list.add(id);
        }
        CatalogEntry e = get(kind, id);
        if (e != null) {
            if (e.hasZh()) {
                list.add(e.name_zh);
            }
            if (e.hasEn()) {
                list.add(e.name_en);
            }
            if (e.pinyin != null && !e.pinyin.isEmpty()) {
                list.add(e.pinyin);
            }
            if (e.initials != null && !e.initials.isEmpty()) {
                list.add(e.initials);
            }
        }
        if (extra != null) {
            for (String s : extra) {
                if (s != null && !s.isEmpty()) {
                    list.add(s);
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    public static List<String> structuresForDim(String dimKey) {
        LinkedHashMap<String, Boolean> ordered = new LinkedHashMap<>();
        for (String vanilla : DestinationLocator.structuresForDim(dimKey)) {
            ordered.put(vanilla, Boolean.TRUE);
        }
        if (structures.entries != null) {
            for (Map.Entry<String, CatalogEntry> e : structures.entries.entrySet()) {
                if (e.getValue() != null && e.getValue().appliesToDim(dimKey)) {
                    ordered.put(e.getKey(), Boolean.TRUE);
                }
            }
        }
        return new ArrayList<>(ordered.keySet());
    }

    private static boolean syncDimensions() {
        boolean changed = false;
        try {
            java.util.List<String> keys = new ArrayList<>();
            keys.add(DimUtil.OVERWORLD);
            keys.add(DimUtil.NETHER);
            keys.add(DimUtil.END);
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                keys.addAll(DimUtil.allLevelKeys(ServerLifecycleHooks.getCurrentServer()));
            }
            for (String key : keys) {
                String norm = DimUtil.normalize(key);
                if (!dimensions.entries.containsKey(norm)) {
                    CatalogEntry entry = new CatalogEntry();
                    seedDimension(norm, entry);
                    dimensions.entries.put(norm, entry);
                    changed = true;
                }
            }
        } catch (Throwable t) {
            DoraAmo.logger.warn("Dimension catalog sync failed: {}", t.toString());
        }
        return changed;
    }

    private static boolean syncBiomes() {
        boolean changed = false;
        List<ResourceLocation> keys = new ArrayList<>(ForgeRegistries.BIOMES.getKeys());
        Collections.sort(keys, new java.util.Comparator<ResourceLocation>() {
            @Override
            public int compare(ResourceLocation a, ResourceLocation b) {
                return a.toString().compareToIgnoreCase(b.toString());
            }
        });
        for (ResourceLocation loc : keys) {
            String key = loc.toString();
            if (!biomes.entries.containsKey(key)) {
                CatalogEntry entry = new CatalogEntry();
                seedBiome(key, entry);
                biomes.entries.put(key, entry);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean syncStructures() {
        boolean changed = false;
        for (String id : DestinationLocator.STRUCTURES_OVERWORLD) {
            changed |= ensureStructure(id, Arrays.asList(DimUtil.OVERWORLD));
        }
        for (String id : DestinationLocator.STRUCTURES_NETHER) {
            changed |= ensureStructure(id, Arrays.asList(DimUtil.NETHER));
        }
        for (String id : DestinationLocator.STRUCTURES_END) {
            changed |= ensureStructure(id, Arrays.asList(DimUtil.END));
        }
        return changed;
    }

    private static boolean ensureStructure(String id, List<String> defaultDims) {
        CatalogEntry existing = structures.entries.get(id);
        if (existing == null) {
            CatalogEntry entry = new CatalogEntry();
            entry.dims = new ArrayList<>(defaultDims);
            seedStructure(id, entry);
            structures.entries.put(id, entry);
            return true;
        }
        if ((existing.dims == null || existing.dims.isEmpty()) && defaultDims != null && !defaultDims.isEmpty()) {
            existing.dims = new ArrayList<>(defaultDims);
            return true;
        }
        return false;
    }

    private static void seedDefaultsIfEmpty() {
        if (dimensions.entries.isEmpty()) {
            seedDimension(DimUtil.OVERWORLD, putNew(dimensions, DimUtil.OVERWORLD));
            seedDimension(DimUtil.NETHER, putNew(dimensions, DimUtil.NETHER));
            seedDimension(DimUtil.END, putNew(dimensions, DimUtil.END));
        }
        if (structures.entries.isEmpty()) {
            syncStructures();
        }
    }

    private static CatalogEntry putNew(CatalogFile file, String key) {
        CatalogEntry e = new CatalogEntry();
        file.entries.put(key, e);
        return e;
    }

    private static void seedDimension(String id, CatalogEntry e) {
        if (DimUtil.OVERWORLD.equals(id) || "0".equals(id)) {
            fill(e, "主世界", "Overworld", "zhushijie", "zsj");
        } else if (DimUtil.NETHER.equals(id) || "-1".equals(id)) {
            fill(e, "地狱", "Nether", "diyu", "dy");
        } else if (DimUtil.END.equals(id) || "1".equals(id)) {
            fill(e, "末地", "The End", "modi", "md");
        }
    }

    private static void seedBiome(String registryId, CatalogEntry e) {
        String path = registryId.contains(":") ? registryId.substring(registryId.indexOf(':') + 1) : registryId;
        String[] seeded = BIOME_SEEDS.get(path);
        if (seeded != null) {
            fill(e, seeded[0], seeded[1], seeded[2], seeded[3]);
        } else {
            e.name_en = path.replace('_', ' ');
        }
    }

    private static void seedStructure(String id, CatalogEntry e) {
        String[] seeded = STRUCTURE_SEEDS.get(id);
        if (seeded != null) {
            fill(e, seeded[0], seeded[1], seeded[2], seeded[3]);
        }
    }

    private static void fill(CatalogEntry e, String zh, String en, String py, String ini) {
        if (!e.hasZh()) {
            e.name_zh = zh;
        }
        if (!e.hasEn()) {
            e.name_en = en;
        }
        if (e.pinyin == null || e.pinyin.isEmpty()) {
            e.pinyin = py;
        }
        if (e.initials == null || e.initials.isEmpty()) {
            e.initials = ini;
        }
    }

    private static CatalogFile fileFor(Kind kind) {
        switch (kind) {
            case DIMENSION:
                return dimensions;
            case BIOME:
                return biomes;
            case STRUCTURE:
                return structures;
            default:
                return null;
        }
    }

    private static CatalogFile loadOrCreate(Kind kind) {
        File file = new File(catalogDir, kind.fileName);
        if (file.isFile()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                CatalogFile loaded = GSON.fromJson(reader, CatalogFile.class);
                if (loaded != null) {
                    if (loaded.entries == null) {
                        loaded.entries = new LinkedHashMap<String, CatalogEntry>();
                    }
                    return loaded;
                }
            } catch (Exception e) {
                DoraAmo.logger.warn("Failed to read {}, regenerating: {}", file.getName(), e.toString());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        CatalogFile created = new CatalogFile();
        created.entries = new LinkedHashMap<String, CatalogEntry>();
        return created;
    }

    private static void saveAll() {
        save(Kind.DIMENSION, dimensions);
        save(Kind.BIOME, biomes);
        save(Kind.STRUCTURE, structures);
    }

    private static void save(Kind kind, CatalogFile data) {
        if (catalogDir == null || data == null) {
            return;
        }
        File file = new File(catalogDir, kind.fileName);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            GSON.toJson(data, writer);
            writer.flush();
        } catch (Exception e) {
            DoraAmo.logger.warn("Failed to write {}: {}", file.getName(), e.toString());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void writeReadme() {
        File readme = new File(catalogDir, "README.txt");
        if (readme.isFile()) {
            return;
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(readme), StandardCharsets.UTF_8));
            writer.write("DoraAmo display catalog\r\n");
            writer.write("======================\r\n");
            writer.write("dimensions.json  key = dimension id (0, -1, 7, ...)\r\n");
            writer.write("biomes.json      key = registry id (minecraft:plains)\r\n");
            writer.write("structures.json  key = structure name (Village or modid:Name)\r\n");
            writer.write("\r\n");
            writer.write("Fields per entry:\r\n");
            writer.write("  name_zh   Chinese display name (empty => show id)\r\n");
            writer.write("  name_en   English display name\r\n");
            writer.write("  pinyin    Full pinyin for filter (e.g. pingyuan)\r\n");
            writer.write("  initials  Initials for filter (e.g. py)\r\n");
            writer.write("  dims      Structures only: list of dimension ids; empty = all dims\r\n");
            writer.write("\r\n");
            writer.write("New mods/dimensions/biomes are appended automatically on load.\r\n");
            writer.write("Your edits to existing keys are preserved.\r\n");
        } catch (Exception ignored) {
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static final Map<String, String[]> BIOME_SEEDS = new LinkedHashMap<String, String[]>();
    private static final Map<String, String[]> STRUCTURE_SEEDS = new LinkedHashMap<String, String[]>();

    static {
        seedB("ocean", "海洋", "Ocean", "haiyang", "hy");
        seedB("plains", "平原", "Plains", "pingyuan", "py");
        seedB("desert", "沙漠", "Desert", "shamo", "sm");
        seedB("extreme_hills", "峭壁", "Extreme Hills", "qiaobi", "qb");
        seedB("forest", "森林", "Forest", "senlin", "sl");
        seedB("taiga", "针叶林", "Taiga", "zhenyelin", "zyl");
        seedB("swampland", "沼泽", "Swampland", "zhaoze", "zz");
        seedB("river", "河流", "River", "heliu", "hl");
        seedB("hell", "下界", "Nether", "xiajie", "xj");
        seedB("sky", "末地", "The End", "modi", "md");
        seedB("frozen_ocean", "冻洋", "Frozen Ocean", "dongyang", "dy");
        seedB("frozen_river", "冻河", "Frozen River", "donghe", "dh");
        seedB("ice_flats", "雪原", "Ice Plains", "xueyuan", "xy");
        seedB("ice_mountains", "雪山", "Ice Mountains", "xueshan", "xs");
        seedB("mushroom_island", "蘑菇岛", "Mushroom Island", "mogudao", "mgd");
        seedB("mushroom_island_shore", "蘑菇岛岸", "Mushroom Island Shore", "mogudaoan", "mgda");
        seedB("beaches", "沙滩", "Beach", "shatan", "st");
        seedB("desert_hills", "沙漠丘陵", "Desert Hills", "shamoqiuling", "smql");
        seedB("forest_hills", "森林丘陵", "Forest Hills", "senlinqiuling", "slql");
        seedB("taiga_hills", "针叶林丘陵", "Taiga Hills", "zhenyelinqiuling", "zylql");
        seedB("smaller_extreme_hills", "峭壁边缘", "Extreme Hills Edge", "qiaobibianyuan", "qbby");
        seedB("jungle", "丛林", "Jungle", "conglin", "cl");
        seedB("jungle_hills", "丛林丘陵", "Jungle Hills", "conglinqiuling", "clql");
        seedB("jungle_edge", "丛林边缘", "Jungle Edge", "conglinbianyuan", "clby");
        seedB("deep_ocean", "深海", "Deep Ocean", "shenhai", "sh");
        seedB("stone_beach", "石岸", "Stone Beach", "shian", "sa");
        seedB("cold_beach", "冷沙滩", "Cold Beach", "lengshatan", "lst");
        seedB("birch_forest", "桦木森林", "Birch Forest", "huamusenlin", "hmsl");
        seedB("birch_forest_hills", "桦木丘陵", "Birch Forest Hills", "huamuqiuling", "hmql");
        seedB("roofed_forest", "黑森林", "Roofed Forest", "heisenlin", "hsl");
        seedB("taiga_cold", "冷针叶林", "Cold Taiga", "lengzhenyelin", "lzyl");
        seedB("taiga_cold_hills", "冷针叶林丘陵", "Cold Taiga Hills", "lengzhenyelinqiuling", "lzylql");
        seedB("redwood_taiga", "巨型针叶林", "Mega Taiga", "juxingzhenyelin", "jxzyl");
        seedB("redwood_taiga_hills", "巨型针叶林丘陵", "Mega Taiga Hills", "juxingzhenyelinqiuling", "jxzylql");
        seedB("extreme_hills_with_trees", "树木峭壁", "Extreme Hills+", "shumuqiaobi", "smqb");
        seedB("savanna", "热带草原", "Savanna", "redaocaoyuan", "rdcy");
        seedB("savanna_rock", "热带高原", "Savanna Plateau", "redaogaoyuan", "rdgy");
        seedB("mesa", "平顶山", "Mesa", "pingdingshan", "pds");
        seedB("mesa_rock", "平顶山森林", "Mesa Plateau F", "pingdingshansenlin", "pdssl");
        seedB("mesa_clear_rock", "平顶山高原", "Mesa Plateau", "pingdingshangaoyuan", "pdsgy");
        seedB("void", "虚空", "The Void", "xukong", "xk");

        seedS("Village", "村庄", "Village", "cunzhuang", "cz");
        seedS("Monument", "海底神殿", "Ocean Monument", "haidishendian", "hdsd");
        seedS("Mansion", "林地府邸", "Woodland Mansion", "lindifudi", "ldfd");
        seedS("Temple", "神庙", "Temple", "shenmiao", "sm");
        seedS("Mineshaft", "废弃矿井", "Mineshaft", "feiqikuangjing", "fqkj");
        seedS("Stronghold", "要塞", "Stronghold", "yaosai", "ys");
        seedS("Fortress", "下界要塞", "Nether Fortress", "xiajieyaosai", "xjys");
        seedS("EndCity", "末地城", "End City", "modicheng", "mdc");
    }

    private static void seedB(String path, String zh, String en, String py, String ini) {
        BIOME_SEEDS.put(path, new String[] { zh, en, py, ini });
    }

    private static void seedS(String id, String zh, String en, String py, String ini) {
        STRUCTURE_SEEDS.put(id, new String[] { zh, en, py, ini });
    }
}
