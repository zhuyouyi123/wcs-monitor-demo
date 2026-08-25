package com.wcs.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wcs.monitor.entity.SysConfig;

import java.util.Map;

public interface SysConfigService extends IService<SysConfig> {

    Map<String, String> getAll();

    void saveConfig(String key, String value);
}
