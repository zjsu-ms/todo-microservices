package com.zjgsu.gateway.filter;

import com.zjgsu.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * JWT认证全局过滤器
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    // 白名单：不需要认证的路径
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 1. 白名单放行
        if (isWhiteListed(path)) {
            log.info("白名单路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 2. 获取Token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("请求未携带Token或格式错误: {}", path);
            return unauthorized(exchange);
        }

        try {
            // 3. 提取并验证JWT
            String jwt = token.substring(7);
            Claims claims = jwtUtil.parseToken(jwt);

            // 4. 添加用户信息到请求头，传递给下游服务
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-Username", claims.get("username", String.class))
                    .header("X-User-Role", claims.get("role", String.class))
                    .build();

            log.info("Token验证成功 - 用户: {}, 路径: {}", claims.get("username"), path);

            // 5. 继续处理请求
            return chain.filter(exchange.mutate().request(request).build());

        } catch (Exception e) {
            log.error("Token验证失败: {}, 路径: {}", e.getMessage(), path);
            return unauthorized(exchange);
        }
    }

    /**
     * 检查是否在白名单中
     */
    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 返回401未授权
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100; // 优先级高，先执行认证
    }
}
