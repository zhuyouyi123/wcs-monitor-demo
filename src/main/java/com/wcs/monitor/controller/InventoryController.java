package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.entity.Inventory;
import com.wcs.monitor.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public Result<List<Inventory>> list() {
        return Result.ok(inventoryService.list());
    }

    @GetMapping("/page")
    public Result<IPage<Inventory>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size) {
        return Result.ok(inventoryService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<Inventory> getById(@PathVariable Long id) {
        return Result.ok(inventoryService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Inventory entity) {
        return Result.ok(inventoryService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Inventory entity) {
        return Result.ok(inventoryService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(inventoryService.removeById(id));
    }
}
