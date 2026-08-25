package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.SysConfig;
import com.wcs.monitor.mapper.SysConfigMapper;
import com.wcs.monitor.service.SysConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "systemName", "warehouseCode", "pageSize",
            "connectTimeout", "heartbeatInterval", "autoReconnect", "reconnectTimes",
            "autoDispatch", "dispatchInterval", "maxTaskPerDevice",
            "refreshInterval", "alarmSound", "opLogKeepDays", "alarmLogKeepDays"
    );

    @Override
    public Map<String, String> getAll() {
        Map<String, String> result = new LinkedHashMap<>();
        lambdaQuery().orderByAsc(SysConfig::getId)
                .list()
                .forEach(c -> result.put(c.getConfigKey(), c.getConfigValue()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(String key, String value) {
        if (key == null || !ALLOWED_KEYS.contains(key)) {
            throw new IllegalArgumentException("不支持的配置项: " + key);
        }
        if (value == null || value.length() > 200) {
            throw new IllegalArgumentException("配置值不合法");
        }
        SysConfig config = lambdaQuery()
                .eq(SysConfig::getConfigKey, key)
                .one();
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            save(config);
        } else {
            config.setConfigValue(value);
            updateById(config);
        }
    }
}
