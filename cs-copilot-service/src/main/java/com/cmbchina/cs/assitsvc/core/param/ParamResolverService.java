package com.cmbchina.cs.assitsvc.core.param;

import com.cmbchina.cs.assitsvc.domain.ItemParam;
import com.cmbchina.cs.assitsvc.domain.ParamContext;

import java.util.List;

/**
 * 参数解析服务。
 */
public interface ParamResolverService {

    /**
     * 解析功能参数。
     *
     * @param paramList 参数配置列表
     * @param ctx       参数上下文
     * @param targetUrl 目标 URL，用于 Cookie 域名绑定校验
     * @return 参数解析结果
     */
    ParamResolveResult resolveParams(List<ItemParam> paramList, ParamContext ctx, String targetUrl);
}
