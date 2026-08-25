package com.wcs.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wcs.monitor.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    boolean saveUser(SysUser user);

    boolean updateUser(SysUser user);

    boolean updateStatus(Long id, Integer status, Long operatorId);

    boolean deleteUser(Long id, Long operatorId);
}
