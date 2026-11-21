package com.caramel.chat.config;

import com.caramel.chat.web.ErrorResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Restricts browser access to the frontend origin only (spec FR-014, constitution IV).
 * Requests from any other browser origin are rejected with HTTP 403 and a user-safe body.
 */
@Configuration
public class CorsConfig implements WebFluxConfigurer {

    private static final String API_PATH = "/api/";

    private final ChatProperties props;
    private final ObjectMapper objectMapper;

    public CorsConfig(ChatProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(API_PATH + "**")
                .allowedOrigins(props.allowedOrigin())
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("Content-Type");
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    WebFilter originGuardFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String path = exchange.getRequest().getPath().value();
            String origin = exchange.getRequest().getHeaders().getOrigin();
            boolean apiCall = path.startsWith(API_PATH);
            // Requests with no Origin header are non-browser clients (for example curl) and are not
            // a CORS concern; only browser requests from a foreign origin are refused.
            if (apiCall && origin != null && !origin.equals(props.allowedOrigin())) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                byte[] body;
                try {
                    body = objectMapper.writeValueAsBytes(new ErrorResponse("Request not allowed."));
                } catch (Exception e) {
                    body = "{\"error\":\"Request not allowed.\"}".getBytes(StandardCharsets.UTF_8);
                }
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            }
            return chain.filter(exchange);
        };
    }
}
