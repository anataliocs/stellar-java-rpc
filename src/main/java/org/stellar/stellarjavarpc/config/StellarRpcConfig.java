package org.stellar.stellarjavarpc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.SorobanServer;

@SuppressWarnings("ClassWithoutLogger")
@Configuration
public class StellarRpcConfig {

    @Value("${stellar.rpc.server.url}")
    private String rpcServerUrl;

    @Bean
    public SorobanServer sorobanServer() {
        return new SorobanServer(rpcServerUrl);
    }

}