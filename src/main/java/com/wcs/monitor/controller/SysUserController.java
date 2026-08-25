package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.common.TokenStore;
import com.wcs.monitor.config.AuthInterceptor;
import com.wcs.monitor.entity.SysUser;
import com.wcs.monitor.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping
    public Result<List<SysUser>> list() {
        List<SysUser> users = sysUserService.list(Wrappers.<SysUser>lambdaQuery()
                .orderByAsc(SysUser::getId));
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody SysUser user) {
        try {
            return Result.ok(sysUserService.saveUser(user));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        try {
            return Result.ok(sysUserService.updateUser(user));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id,
                                        @RequestParam Integer status,
                                        HttpServletRequest request) {
        try {
            return Result.ok(sysUserService.updateStatus(id, status, currentUserId(request)));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        try {
            return Result.ok(sysUserService.deleteUser(id, currentUserId(request)));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    private Long currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthInterceptor.SESSION_ATTR);
        return attr instanceof TokenStore.Session session ? session.userId() : null;
    }
}
