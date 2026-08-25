package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.SysDictItem;
import com.wcs.monitor.mapper.SysDictItemMapper;
import com.wcs.monitor.service.SysDictItemService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysDictItemServiceImpl extends ServiceImpl<SysDictItemMapper, SysDictItem> implements SysDictItemService {

    @Override
    public void saveItem(SysDictItem item) {
        validate(item);
        item.setId(null);
        checkDuplicate(item);
        if (item.getSortOrder() == null) {
            item.setSortOrder(0);
        }
        save(item);
    }

    @Override
    public void updateItem(SysDictItem item) {
        if (getById(item.getId()) == null) {
            throw new IllegalArgumentException("字典项不存在");
        }
        validate(item);
        checkDuplicate(item);
        updateById(item);
    }

    @Override
    public List<Map<String, Object>> listGroups() {
        Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
        List<SysDictItem> all = list();
        for (SysDictItem item : all) {
            Map<String, Object> group = groups.get(item.getDictKey());
            if (group == null) {
                group = new LinkedHashMap<>();
                group.put("dictKey", item.getDictKey());
                group.put("dictName", item.getDictName());
                group.put("count", 0);
                groups.put(item.getDictKey(), group);
            }
            group.put("count", (Integer) group.get("count") + 1);
        }
        return List.copyOf(groups.values());
    }

    @Override
    public Map<String, String> labelMap(String dictKey) {
        if (dictKey == null || dictKey.isBlank()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        lambdaQuery()
                .eq(SysDictItem::getDictKey, dictKey.trim())
                .orderByAsc(SysDictItem::getSortOrder)
                .list()
                .forEach(item -> result.put(item.getDictValue(), item.getDictLabel()));
        return result;
    }

    @Override
    public Map<String, DictValueInfo> valueInfoMap(String dictKey) {
        if (dictKey == null || dictKey.isBlank()) {
            return Map.of();
        }
        Map<String, DictValueInfo> result = new LinkedHashMap<>();
        lambdaQuery()
                .eq(SysDictItem::getDictKey, dictKey.trim())
                .orderByAsc(SysDictItem::getSortOrder)
                .list()
                .forEach(item -> result.put(item.getDictValue(),
                        new DictValueInfo(item.getDictLabel(), item.getDictColor())));
        return result;
    }

    private void validate(SysDictItem item) {
        if (item.getDictName() == null || item.getDictName().isBlank()) {
            throw new IllegalArgumentException("字典名称不能为空");
        }
        if (item.getDictKey() == null || item.getDictKey().isBlank()) {
            throw new IllegalArgumentException("字典键不能为空");
        }
        if (item.getDictValue() == null || item.getDictValue().isBlank()) {
            throw new IllegalArgumentException("字典值不能为空");
        }
        if (item.getDictLabel() == null || item.getDictLabel().isBlank()) {
            throw new IllegalArgumentException("含义不能为空");
        }
        item.setDictName(item.getDictName().trim());
        item.setDictKey(item.getDictKey().trim());
        item.setDictValue(item.getDictValue().trim());
        item.setDictLabel(item.getDictLabel().trim());
        if (item.getDictColor() == null || item.getDictColor().isBlank()) {
            item.setDictColor(null);
        } else if (!item.getDictColor().trim().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("颜色格式不正确，需为 #RRGGBB 格式");
        } else {
            item.setDictColor(item.getDictColor().trim().toUpperCase());
        }
    }

    private void checkDuplicate(SysDictItem item) {
        Long count = lambdaQuery()
                .eq(SysDictItem::getDictKey, item.getDictKey())
                .eq(SysDictItem::getDictValue, item.getDictValue())
                .ne(item.getId() != null, SysDictItem::getId, item.getId())
                .count();
        if (count != null && count > 0) {
            throw new IllegalArgumentException(
                    "字典[" + item.getDictName() + "]下已存在值 " + item.getDictValue());
        }
    }
}
