package com.seven;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class ESConfig {

    @Bean
    public TransportClient client() throws UnknownHostException {

        // 这里默认配置一个es的集群名字
        Settings settings = Settings.builder()
                .put("cluster.name", "elasticsearch").build();

        // 节点
        InetAddress byName = InetAddress.getByName("127.0.0.1");
        TransportAddress node = new TransportAddress(byName,9300);

        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(node);

        return client;
    }
}
