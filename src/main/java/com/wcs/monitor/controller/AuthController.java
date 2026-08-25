package com.wcs.monitor.controller;

import com.wcs.monitor.common.Result;
import com.wcs.monitor.common.TokenStore;
import com.wcs.monitor.entity.SysUser;
import com.wcs.monitor.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final TokenStore tokenStore;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        if (username.isEmpty() || password.isEmpty()) {
            return Result.fail("用户名和密码不能为空");
        }
        SysUser user = sysUserService.lambdaQuery()
                .eq(SysUser::getUsername, username)
                .one();
        if (user == null || !user.getPassword().equals(password)) {
            return Result.fail("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail("该账号已被禁用，请联系管理员");
        }
        String token = tokenStore.create(user.getId(), user.getUsername(), user.getRealName(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        return Result.ok(data);
    }

    @PostMapping("/logout")
    public Result<Boolean> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tokenStore.remove(authorization.substring(7));
        }
        return Result.ok(true);
    }
}
