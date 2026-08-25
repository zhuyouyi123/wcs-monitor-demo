package com.wcs.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wcs.monitor.entity.SysDictItem;

import java.util.List;
import java.util.Map;

public interface SysDictItemService extends IService<SysDictItem> {

    /** 字典值信息：含义 + 显示颜色 */
    class DictValueInfo {
        private final String label;
        private final String color;

        public DictValueInfo(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }
    }

    void saveItem(SysDictItem item);

    void updateItem(SysDictItem item);

    List<Map<String, Object>> listGroups();

    /** 指定字典键的 值->含义 映射；键为空或字典不存在时返回空 Map */
    Map<String, String> labelMap(String dictKey);

    /** 指定字典键的 值->(含义,颜色) 映射；键为空或字典不存在时返回空 Map */
    Map<String, DictValueInfo> valueInfoMap(String dictKey);
}
