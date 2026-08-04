package com.doraamo.config.catalog;

import com.doraamo.util.DimUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One editable display/search row in a catalog JSON file. */
public class CatalogEntry {

    public String name_zh = "";
    public String name_en = "";
    public String pinyin = "";
    public String initials = "";
    /** Structure only: dimension keys where this structure is listed. Empty = all dims. */
    public List<String> dims = new ArrayList<>();

    public CatalogEntry() {
    }

    public boolean hasZh() {
        return name_zh != null && !name_zh.trim().isEmpty();
    }

    public boolean hasEn() {
        return name_en != null && !name_en.trim().isEmpty();
    }

    @Nullable
    public String pickName(boolean preferChinese) {
        if (preferChinese && hasZh()) {
            return name_zh.trim();
        }
        if (hasEn()) {
            return name_en.trim();
        }
        if (hasZh()) {
            return name_zh.trim();
        }
        return null;
    }

    public List<String> dimsOrEmpty() {
        return dims == null ? Collections.emptyList() : dims;
    }

    public boolean appliesToDim(String dimKey) {
        List<String> list = dimsOrEmpty();
        if (list.isEmpty()) {
            return true;
        }
        String norm = DimUtil.normalize(dimKey);
        for (String d : list) {
            if (d != null && DimUtil.normalize(d).equals(norm)) {
                return true;
            }
            try {
                if (DimUtil.fromLegacyInt(Integer.parseInt(d)).equals(norm)) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }
}
