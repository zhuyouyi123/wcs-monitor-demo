package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.SysUser;
import com.wcs.monitor.mapper.SysUserMapper;
import com.wcs.monitor.service.SysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final String BUILTIN_ADMIN = "admin";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveUser(SysUser user) {
        validate(user, null);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        return save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUser user) {
        SysUser old = getById(user.getId());
        if (old == null) {
            throw new IllegalArgumentException("用户不存在: " + user.getId());
        }
        validate(user, user.getId());
        if (BUILTIN_ADMIN.equals(old.getUsername()) && !"admin".equals(user.getRole())) {
            throw new IllegalArgumentException("内置管理员角色不可修改");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(null);
        }
        return updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status, Long operatorId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态值不合法");
        }
        checkOperable(id, operatorId);
        return lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(SysUser::getStatus, status)
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long id, Long operatorId) {
        checkOperable(id, operatorId);
        return removeById(id);
    }

    private void checkOperable(Long id, Long operatorId) {
        SysUser user = getById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        if (id.equals(operatorId)) {
            throw new IllegalArgumentException("不能对自己执行该操作");
        }
        if (BUILTIN_ADMIN.equals(user.getUsername())) {
            throw new IllegalArgumentException("内置管理员不可删除或禁用");
        }
    }

    private void validate(SysUser user, Long excludeId) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        String role = user.getRole();
        if (role != null && !"admin".equals(role) && !"user".equals(role)) {
            throw new IllegalArgumentException("角色不合法，仅支持 admin / user");
        }
        boolean exists = lambdaQuery()
                .eq(SysUser::getUsername, user.getUsername())
                .ne(excludeId != null, SysUser::getId, excludeId)
                .exists();
        if (exists) {
            throw new IllegalArgumentException("用户名「" + user.getUsername() + "」已存在");
        }
    }
}
