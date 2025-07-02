package com.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.config.ApplicationConfig;
import com.notification.model.SmsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchService {

    private final RestHighLevelClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    public void indexSmsRequest(SmsRequest smsRequest) {
        try {
            Map<String, Object> document = objectMapper.convertValue(smsRequest, Map.class);
            
            // Convert LocalDateTime to string for Elasticsearch
            if (document.containsKey("createdAt")) {
                document.put("createdAt", smsRequest.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            if (document.containsKey("updatedAt")) {
                document.put("updatedAt", smsRequest.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            IndexRequest indexRequest = new IndexRequest(ApplicationConfig.SMS_INDEX)
                    .id(smsRequest.getCorrelationId())
                    .source(document, XContentType.JSON);

            elasticsearchClient.index(indexRequest, RequestOptions.DEFAULT);
            log.info("Successfully indexed SMS request with correlation ID: {}", smsRequest.getCorrelationId());
            
        } catch (IOException e) {
            log.error("Error indexing SMS request with correlation ID: {}", smsRequest.getCorrelationId(), e);
            throw new RuntimeException("Failed to index SMS request", e);
        }
    }

    public Page<SmsRequest> searchSmsByPhoneNumberAndTimeRange(String phoneNumber, LocalDateTime startTime, 
                                                              LocalDateTime endTime, Pageable pageable) {
        try {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("phoneNumber", phoneNumber))
                    .must(QueryBuilders.rangeQuery("createdAt")
                            .gte(startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                            .lte(endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

            SearchRequest searchRequest = buildSearchRequest(queryBuilder, pageable);
            SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
            
            return parseSearchResponse(response, pageable);
            
        } catch (IOException e) {
            log.error("Error searching SMS by phone number and time range: {}", phoneNumber, e);
            throw new RuntimeException("Failed to search SMS requests", e);
        }
    }

    public Page<SmsRequest> searchSmsByText(String text, Pageable pageable) {
        try {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchQuery("message", text));

            SearchRequest searchRequest = buildSearchRequest(queryBuilder, pageable);
            SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
            
            return parseSearchResponse(response, pageable);
            
        } catch (IOException e) {
            log.error("Error searching SMS by text: {}", text, e);
            throw new RuntimeException("Failed to search SMS requests", e);
        }
    }

    private SearchRequest buildSearchRequest(BoolQueryBuilder queryBuilder, Pageable pageable) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.sort("createdAt", SortOrder.DESC);
        searchSourceBuilder.from((int) pageable.getOffset());
        searchSourceBuilder.size(pageable.getPageSize());

        return new SearchRequest(ApplicationConfig.SMS_INDEX)
                .source(searchSourceBuilder);
    }

    private Page<SmsRequest> parseSearchResponse(SearchResponse response, Pageable pageable) {
        List<SmsRequest> smsRequests = new ArrayList<>();
        
        for (SearchHit hit : response.getHits().getHits()) {
            try {
                Map<String, Object> source = hit.getSourceAsMap();
                SmsRequest smsRequest = objectMapper.convertValue(source, SmsRequest.class);
                smsRequests.add(smsRequest);
            } catch (Exception e) {
                log.error("Error parsing search hit: {}", hit.getId(), e);
            }
        }

        long totalHits = response.getHits().getTotalHits().value;
        return new PageImpl<>(smsRequests, pageable, totalHits);
    }

    public boolean isHealthy() {
        try {
            // Simple ping to check if Elasticsearch is responding
            return elasticsearchClient.ping(RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Elasticsearch health check failed", e);
            return false;
        }
    }
} 