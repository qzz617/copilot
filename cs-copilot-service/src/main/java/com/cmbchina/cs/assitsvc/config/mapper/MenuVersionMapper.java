package com.cmbchina.cs.assitsvc.config.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * cs_menu_version 配置发布表查询 Mapper。
 */
@Mapper
public interface MenuVersionMapper {

    @Select("SELECT config_data FROM svccfg.cs_menu_version ORDER BY created_time DESC LIMIT 1")
    String selectLatestConfigData();

    @Select("SELECT version FROM svccfg.cs_menu_version ORDER BY created_time DESC LIMIT 1")
    String selectLatestVersion();
}
