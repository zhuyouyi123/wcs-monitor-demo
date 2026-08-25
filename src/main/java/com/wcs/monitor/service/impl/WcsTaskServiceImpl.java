package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.WcsTask;
import com.wcs.monitor.mapper.WcsTaskMapper;
import com.wcs.monitor.service.WcsTaskService;
import org.springframework.stereotype.Service;

@Service
public class WcsTaskServiceImpl extends ServiceImpl<WcsTaskMapper, WcsTask> implements WcsTaskService {
}
