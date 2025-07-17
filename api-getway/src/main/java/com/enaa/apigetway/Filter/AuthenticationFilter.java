package com.enaa.apigetway.Filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AuthenticationFilter   extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidateur validator;
    private final WebClient.Builder webClientBuilder;

    public AuthenticationFilter(RouteValidateur validator, WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.validator = validator;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (validator.isSecured.test(exchange.getRequest())) {
                //header contains token or not
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw new RuntimeException("missing authorization header");
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                }
                try {
                    //REST call to AUTH service
                    webClientBuilder.build().get()
                            .uri("http://auth-service/auth/validate?token=" + authHeader)
                            .retrieve().bodyToMono(String.class)
                            .map(response -> {
                                System.out.println(response);
                                return exchange;
                            }).block();
                } catch (Exception e) {
                    System.out.println("invalid access...!");
                    throw new RuntimeException("un authorized access to application");
                }
            }
            return chain.filter(exchange);
        });
    }

    public static class Config {

    }
}
