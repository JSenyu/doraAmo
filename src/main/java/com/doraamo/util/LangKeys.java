package com.doraamo.util;

/** Translation key constants for DoraAmo. */
public final class LangKeys {

    public static final String DIMENSION_BLANK = "doraamo.dimension.blank";
    public static final String DIMENSION_UNKNOWN = "doraamo.dimension.unknown";
    public static final String DIMENSION_PREFIX = "doraamo.dimension.";

    public static final String MODE_PREFIX = "doraamo.mode.";

    public static final String PORTAL_DEST_SCALED = "doraamo.portal.dest.scaled";
    public static final String PORTAL_NO_SAFE_LANDING = "doraamo.portal.msg.no_safe_landing";
    public static final String PORTAL_TARGET_BLOCKED = "doraamo.portal.msg.target_blocked";

    public static final String TUNER_SUB_LOCKED = "doraamo.tuner.msg.sub_locked";
    public static final String TUNER_APPLIED = "doraamo.tuner.msg.applied";
    public static final String TUNER_OVERWRITE = "doraamo.tuner.msg.overwrite";
    public static final String TUNER_NO_NEARBY = "doraamo.tuner.msg.no_nearby";
    public static final String TUNER_NEAREST = "doraamo.tuner.msg.nearest";
    public static final String TUNER_TYPE_MAIN = "doraamo.tuner.type.main";
    public static final String TUNER_TYPE_SUB = "doraamo.tuner.type.sub";

    public static final String GUI_TITLE = "doraamo.gui.tuner.title";
    public static final String GUI_STATUS = "doraamo.gui.tuner.status";
    public static final String GUI_SAVE = "doraamo.gui.tuner.save";
    public static final String GUI_FORCE_SAVE = "doraamo.gui.tuner.force_save";
    public static final String GUI_EXPLORE = "doraamo.gui.tuner.explore";
    public static final String GUI_FILTER_HINT = "doraamo.gui.tuner.filter_hint";
    public static final String GUI_DIM_LIST = "doraamo.gui.tuner.dim_list";
    public static final String GUI_STATUS_NEED_SEARCH = "doraamo.gui.tuner.status.need_search";
    public static final String GUI_STATUS_SEARCHING = "doraamo.gui.tuner.status.searching";
    public static final String GUI_STATUS_FOUND_SAFE = "doraamo.gui.tuner.status.found_safe";
    public static final String GUI_STATUS_FOUND_UNSAFE = "doraamo.gui.tuner.status.found_unsafe";
    public static final String GUI_STATUS_NOT_FOUND = "doraamo.gui.tuner.status.not_found";
    public static final String GUI_STATUS_SAVE_BLOCKED = "doraamo.gui.tuner.status.save_blocked";
    public static final String GUI_BOUND_NONE = "doraamo.gui.tuner.bound.none";
    public static final String GUI_BOUND_CURRENT = "doraamo.gui.tuner.bound.current";
    public static final String GUI_BOUND_OVERWRITE_HINT = "doraamo.gui.tuner.bound.overwrite_hint";
    public static final String GUI_BOUND_COORDS = "doraamo.gui.tuner.bound.coords";
    public static final String GUI_BOUND_BIOME = "doraamo.gui.tuner.bound.biome";
    public static final String GUI_BOUND_STRUCTURE = "doraamo.gui.tuner.bound.structure";
    public static final String GUI_HAZARD_NONE = "doraamo.gui.tuner.hazard.none";
    public static final String GUI_HAZARD_FLOATING = "doraamo.gui.tuner.hazard.floating";
    public static final String GUI_HAZARD_WALL = "doraamo.gui.tuner.hazard.wall";
    public static final String GUI_HAZARD_FLOODED = "doraamo.gui.tuner.hazard.flooded";
    public static final String GUI_HAZARD_LAVA = "doraamo.gui.tuner.hazard.lava";
    public static final String GUI_HAZARD_FIRE = "doraamo.gui.tuner.hazard.fire";
    public static final String GUI_HAZARD_BOUNDS = "doraamo.gui.tuner.hazard.bounds";
    public static final String BIOME_UNKNOWN = "doraamo.biome.unknown";

    private LangKeys() {
    }

    public static String dimensionKey(String dimensionKey) {
        return DIMENSION_PREFIX + dimensionKey.replace(':', '.');
    }

    public static String modeKey(String modeName) {
        return MODE_PREFIX + modeName.toLowerCase();
    }
}
