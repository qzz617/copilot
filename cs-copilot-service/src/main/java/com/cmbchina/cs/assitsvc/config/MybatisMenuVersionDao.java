package com.cmbchina.cs.assitsvc.config;

import com.cmbchina.cs.assitsvc.config.mapper.MenuVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 基于 MyBatis 的菜单版本 CLOB 读取 DAO。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MybatisMenuVersionDao implements MenuVersionDao {

    private final MenuVersionMapper menuVersionMapper;

    @Override
    public String fetchLatestActiveVersion() {
        return menuVersionMapper.selectLatestConfigData();
    }

    @Override
    public String fetchLatestVersionMarker() {
        return menuVersionMapper.selectLatestVersion();
    }
}
