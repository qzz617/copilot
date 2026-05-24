package com.cmbchina.cs.assitsvc.infra.metrics;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * MVP 阶段反馈结果 ES 写入客户端。
 */
@Slf4j
@Component
public class FeedbackEsClient {

    private final FeedbackEsProperties properties;
    private final ObjectProvider<RestHighLevelClient> clientProvider;

    public FeedbackEsClient(FeedbackEsProperties properties,
                            @Qualifier("feedbackRestHighLevelClient")
                            ObjectProvider<RestHighLevelClient> clientProvider) {
        this.properties = properties;
        this.clientProvider = clientProvider;
    }

    /**
     * 写入反馈结果到 ES。失败抛出异常，由调用方决定是否阻塞。
     *
     * @param record 反馈记录
     */
    public void index(FeedbackLogRecord record) throws IOException {
        if (record == null || !properties.isEnabled()) {
            return;
        }
        RestHighLevelClient client = clientProvider.getIfAvailable();
        if (client == null) {
            log.warn("[M16] Feedback ES client is unavailable, logId={}", record.getLogId());
            return;
        }

        IndexRequest request = new IndexRequest(properties.getIndexName())
                .id(record.getLogId())
                .source(JSON.toJSONString(record), XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
        log.debug("[M16] Feedback result indexed to ES, logId={}", record.getLogId());
    }
}
