package com.cmbchina.cs.assitsvc.config;

/**
 * 菜单版本 CLOB 读取 DAO。
 */
public interface MenuVersionDao {

    /**
     * 查询最新生效版本的 CLOB JSON。
     *
     * @return CLOB JSON；不存在时返回 null
     */
    String fetchLatestActiveVersion();

    /**
     * 查询最新生效版本标识，用于多 Pod 轮询判断是否需要刷新。
     *
     * @return 版本标识；不存在时返回 null
     */
    String fetchLatestVersionMarker();
}
