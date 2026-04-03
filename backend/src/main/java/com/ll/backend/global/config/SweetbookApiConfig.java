package com.ll.backend.global.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@Slf4j
public class SweetbookApiConfig {
    @Value("${sweetbook.api.key}")
    private String apiKey;

    @Value("${sweetbook.api.base-url}")
    private String baseUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient sweetbookWebClient(WebClient.Builder webClientBuilder)
            throws SSLException {
        // DEV ONLY: TLS에서 모든 서버 인증서를 신뢰합니다. 운영 환경에서는 반드시 정상 PKIX 검증(기본 trustStore 또는 명시적 CA)으로 바꾸세요.
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        HttpClient httpClient = HttpClient.create()
                .secure(ssl -> ssl.sslContext(sslContext));

        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }
}
