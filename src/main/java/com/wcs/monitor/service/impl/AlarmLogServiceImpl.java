package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.AlarmLog;
import com.wcs.monitor.mapper.AlarmLogMapper;
import com.wcs.monitor.service.AlarmLogService;
import org.springframework.stereotype.Service;

@Service
public class AlarmLogServiceImpl extends ServiceImpl<AlarmLogMapper, AlarmLog> implements AlarmLogService {
}
