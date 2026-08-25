package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.entity.SysDictItem;
import com.wcs.monitor.service.SysDictItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dict-items")
@RequiredArgsConstructor
public class SysDictItemController {

    private final SysDictItemService sysDictItemService;

    @GetMapping
    public Result<List<SysDictItem>> list(@RequestParam(required = false) String dictKey) {
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<SysDictItem>()
                .orderByAsc(SysDictItem::getDictKey)
                .orderByAsc(SysDictItem::getSortOrder)
                .orderByAsc(SysDictItem::getId);
        if (dictKey != null && !dictKey.isBlank()) {
            wrapper.eq(SysDictItem::getDictKey, dictKey);
        }
        return Result.ok(sysDictItemService.list(wrapper));
    }

    /** 字典项分页查询，支持按分组与关键字（值/含义模糊）过滤 */
    @GetMapping("/page")
    public Result<IPage<SysDictItem>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "20") long size,
                                           @RequestParam(required = false) String dictKey,
                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<SysDictItem>()
                .eq(dictKey != null && !dictKey.isBlank(), SysDictItem::getDictKey, dictKey)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(SysDictItem::getDictValue, keyword)
                              .or().like(SysDictItem::getDictLabel, keyword))
                .orderByAsc(SysDictItem::getDictKey)
                .orderByAsc(SysDictItem::getSortOrder)
                .orderByAsc(SysDictItem::getId);
        return Result.ok(sysDictItemService.page(new Page<>(current, size), wrapper));
    }

    /** 字典分组列表（去重后的 key + 名称 + 条目数） */
    @GetMapping("/groups")
    public Result<List<Map<String, Object>>> groups() {
        return Result.ok(sysDictItemService.listGroups());
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody SysDictItem item) {
        try {
            sysDictItemService.saveItem(item);
            return Result.ok(true);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SysDictItem item) {
        item.setId(id);
        try {
            sysDictItemService.updateItem(item);
            return Result.ok(true);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(sysDictItemService.removeById(id));
    }
}
