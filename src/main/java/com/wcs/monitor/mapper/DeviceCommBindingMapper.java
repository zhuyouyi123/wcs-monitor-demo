package com.wcs.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wcs.monitor.entity.DeviceCommBinding;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeviceCommBindingMapper extends BaseMapper<DeviceCommBinding> {

    @Insert("<script>" +
            "INSERT INTO device_comm_binding (device_id, config_id, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.deviceId}, #{item.configId}, #{item.createTime})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<DeviceCommBinding> bindings);
}
