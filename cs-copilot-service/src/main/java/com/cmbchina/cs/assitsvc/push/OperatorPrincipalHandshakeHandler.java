package com.cmbchina.cs.assitsvc.push;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Map;

/**
 * 从握手请求中解析 operatorId，并绑定为 Spring WebSocket user。
 */
public class OperatorPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    private final WebSocketProperties properties;

    public OperatorPrincipalHandshakeHandler(WebSocketProperties properties) {
        this.properties = properties;
    }

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String operatorId = resolveOperatorId(request);
        if (StringUtils.hasText(operatorId)) {
            return new OperatorPrincipal(operatorId.trim());
        }
        return request.getPrincipal();
    }

    private String resolveOperatorId(ServerHttpRequest request) {
        String headerName = properties.getOperatorIdHeader();
        if (StringUtils.hasText(headerName)) {
            String headerValue = request.getHeaders().getFirst(headerName);
            if (StringUtils.hasText(headerValue)) {
                return headerValue;
            }
        }

        String queryParamName = properties.getOperatorIdQueryParam();
        if (StringUtils.hasText(queryParamName)) {
            return UriComponentsBuilder.fromUri(request.getURI())
                    .build()
                    .getQueryParams()
                    .getFirst(queryParamName);
        }
        return null;
    }
}
