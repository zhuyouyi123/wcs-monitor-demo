package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.CommTestConfig;
import com.wcs.monitor.mapper.CommTestConfigMapper;
import com.wcs.monitor.service.CommTestConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommTestConfigServiceImpl extends ServiceImpl<CommTestConfigMapper, CommTestConfig>
        implements CommTestConfigService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveConfig(CommTestConfig config) {
        validate(config);
        checkDuplicate(config, null);
        config.setId(null);
        return save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(CommTestConfig config) {
        CommTestConfig old = getById(config.getId());
        if (old == null) {
            throw new IllegalArgumentException("配置不存在: " + config.getId());
        }
        config.setDeviceType(old.getDeviceType());
        validate(config);
        checkDuplicate(config, config.getId());
        return updateById(config);
    }

    private void validate(CommTestConfig config) {
        if (config.getConfigName() == null || config.getConfigName().isBlank()) {
            throw new IllegalArgumentException("配置名称不能为空");
        }
        if (config.getDeviceType() == null) {
            throw new IllegalArgumentException("设备类型不能为空");
        }
        if (config.getDataType() == null) {
            throw new IllegalArgumentException("数据类型不能为空");
        }
        if (config.getDbNumber() == null || config.getDbNumber() < 1 || config.getDbNumber() > 65535) {
            throw new IllegalArgumentException("DB 块号必须在 1-65535 之间");
        }
        if (config.getStartOffset() == null || config.getStartOffset() < 0) {
            throw new IllegalArgumentException("起始偏移不能为负数");
        }
        if (config.getReadLength() == null || config.getReadLength() < 1 || config.getReadLength() > 512) {
            throw new IllegalArgumentException("读取长度必须在 1-512 字节之间");
        }
    }

    private void checkDuplicate(CommTestConfig config, Long excludeId) {
        boolean nameExists = lambdaQuery()
                .eq(CommTestConfig::getDeviceType, config.getDeviceType())
                .eq(CommTestConfig::getConfigName, config.getConfigName())
                .ne(excludeId != null, CommTestConfig::getId, excludeId)
                .exists();
        if (nameExists) {
            throw new IllegalArgumentException(
                    "设备类型「" + config.getDeviceType().getDescription()
                            + "」下已存在同名配置「" + config.getConfigName() + "」");
        }
        boolean paramsDuplicated = lambdaQuery()
                .eq(CommTestConfig::getDeviceType, config.getDeviceType())
                .eq(CommTestConfig::getDbNumber, config.getDbNumber())
                .eq(CommTestConfig::getStartOffset, config.getStartOffset())
                .eq(CommTestConfig::getReadLength, config.getReadLength())
                .eq(CommTestConfig::getDataType, config.getDataType())
                .ne(excludeId != null, CommTestConfig::getId, excludeId)
                .exists();
        if (paramsDuplicated) {
            throw new IllegalArgumentException("已存在相同读取参数的配置（块号/偏移/长度/数据类型完全一致），请勿重复添加");
        }
    }
}
