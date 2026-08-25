package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.entity.ConveyorNode;
import com.wcs.monitor.service.ConveyorNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conveyor-nodes")
@RequiredArgsConstructor
public class ConveyorNodeController {

    private final ConveyorNodeService conveyorNodeService;

    @GetMapping
    public Result<List<ConveyorNode>> list(@RequestParam(required = false) Long deviceId) {
        return Result.ok(conveyorNodeService.list(Wrappers.<ConveyorNode>lambdaQuery()
                .eq(deviceId != null, ConveyorNode::getDeviceId, deviceId)
                .orderByAsc(ConveyorNode::getId)));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody ConveyorNode node) {
        try {
            return Result.ok(conveyorNodeService.saveNode(node));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody ConveyorNode node) {
        try {
            return Result.ok(conveyorNodeService.updateNode(node));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(conveyorNodeService.removeById(id));
    }
}
