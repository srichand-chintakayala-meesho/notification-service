package com.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Consolidated configuration for the Notification Service
 */
@Configuration
public class ApplicationConfig {

    // Kafka Topics
    public static final String SMS_SEND_TOPIC = "notification.send_sms";
    
    // Redis Keys
    public static final String BLACKLIST_KEY = "sms:blacklist";
    
    // Elasticsearch Index
    public static final String SMS_INDEX = "sms_requests";
    
    // Error Codes
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String PHONE_NUMBER_BLACKLISTED = "PHONE_NUMBER_BLACKLISTED";
    public static final String SMS_SEND_FAILED = "SMS_SEND_FAILED";
    public static final String REQUEST_NOT_FOUND = "REQUEST_NOT_FOUND";
    
    // Success Messages
    public static final String SMS_SENT_SUCCESS = "Successfully Sent";
    public static final String BLACKLIST_SUCCESS = "Successfully blacklisted";
    public static final String REMOVE_BLACKLIST_SUCCESS = "Successfully removed from blacklist";
    
    // Third Party API Constants
    public static final String SMS_API_DELIVERY_CHANNEL = "sms";
    public static final String SMS_API_CHANNEL_TYPE = "sms";
    
    // HTTP Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    
    // Cache TTL (in seconds)
    public static final long BLACKLIST_CACHE_TTL = 86400; // 24 hours

    // Elasticsearch Configuration
    @Value("${elasticsearch.host}")
    private String elasticsearchHost;

    @Value("${elasticsearch.port}")
    private int elasticsearchPort;

    @Value("${elasticsearch.scheme}")
    private String elasticsearchScheme;

    @Bean
    @SuppressWarnings("deprecation")
    public RestHighLevelClient elasticsearchClient() {
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort, elasticsearchScheme))
        );
    }

    // Kafka Configuration
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    // RestTemplate Configuration
    @Value("${sms.api.timeout}")
    private int timeout;

    @Bean
    public RestTemplate restTemplate() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        return new RestTemplate(requestFactory);
    }

    // Authentication Filter
    @Component
    @Slf4j
    public static class AuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing authorization header for request: {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Authorization header is required\"}}");
                return;
            }
            
            log.debug("Authorization header present for request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
        }
    }
} 