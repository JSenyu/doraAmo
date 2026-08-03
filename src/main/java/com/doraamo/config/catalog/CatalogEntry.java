package com.doraamo.config.catalog;

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
    /** Structure only: dimension ids where this structure is listed. Empty = all dims. */
    public List<Integer> dims = new ArrayList<Integer>();

    public CatalogEntry() {
    }

    public CatalogEntry(String nameZh, String nameEn, String pinyin, String initials) {
        this.name_zh = nameZh == null ? "" : nameZh;
        this.name_en = nameEn == null ? "" : nameEn;
        this.pinyin = pinyin == null ? "" : pinyin;
        this.initials = initials == null ? "" : initials;
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

    public List<Integer> dimsOrEmpty() {
        return dims == null ? Collections.<Integer>emptyList() : dims;
    }

    public boolean appliesToDim(int dimId) {
        List<Integer> list = dimsOrEmpty();
        if (list.isEmpty()) {
            return true;
        }
        for (Integer d : list) {
            if (d != null && d.intValue() == dimId) {
                return true;
            }
        }
        return false;
    }
}
