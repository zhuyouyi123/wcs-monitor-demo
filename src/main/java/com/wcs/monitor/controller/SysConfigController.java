package com.wcs.monitor.controller;

import com.wcs.monitor.common.Result;
import com.wcs.monitor.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @GetMapping
    public Result<Map<String, String>> getAll() {
        return Result.ok(sysConfigService.getAll());
    }

    @PutMapping("/{key}")
    public Result<Boolean> save(@PathVariable String key, @RequestBody Map<String, String> body) {
        try {
            sysConfigService.saveConfig(key, body.get("value"));
            return Result.ok(true);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
