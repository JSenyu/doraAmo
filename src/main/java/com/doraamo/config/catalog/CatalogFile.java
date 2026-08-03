package com.doraamo.config.catalog;

import java.util.LinkedHashMap;
import java.util.Map;

/** Root object for catalog/*.json */
public class CatalogFile {

    public int version = 1;
    /** id → entry (dimension int id / biome registry name / structure name) */
    public Map<String, CatalogEntry> entries = new LinkedHashMap<String, CatalogEntry>();

    public CatalogFile() {
    }
}
