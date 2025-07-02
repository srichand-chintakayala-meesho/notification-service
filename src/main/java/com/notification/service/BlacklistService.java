package com.notification.service;

import com.notification.config.ApplicationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    public void addToBlacklist(List<String> phoneNumbers) {
        log.info("Adding phone numbers to blacklist: {}", phoneNumbers);
        
        for (String phoneNumber : phoneNumbers) {
            redisTemplate.opsForSet().add(ApplicationConfig.BLACKLIST_KEY, phoneNumber);
        }
        
        // Set TTL for the blacklist
        redisTemplate.expire(ApplicationConfig.BLACKLIST_KEY, ApplicationConfig.BLACKLIST_CACHE_TTL, TimeUnit.SECONDS);
        log.info("Successfully added {} phone numbers to blacklist", phoneNumbers.size());
    }

    public void removeFromBlacklist(List<String> phoneNumbers) {
        log.info("Removing phone numbers from blacklist: {}", phoneNumbers);
        
        for (String phoneNumber : phoneNumbers) {
            redisTemplate.opsForSet().remove(ApplicationConfig.BLACKLIST_KEY, phoneNumber);
        }
        
        log.info("Successfully removed {} phone numbers from blacklist", phoneNumbers.size());
    }

    public Set<String> getBlacklistedNumbers() {
        log.info("Fetching all blacklisted phone numbers");
        return redisTemplate.opsForSet().members(ApplicationConfig.BLACKLIST_KEY);
    }

    public boolean isBlacklisted(String phoneNumber) {
        boolean blacklisted = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ApplicationConfig.BLACKLIST_KEY, phoneNumber));
        if (blacklisted) {
            log.warn("Phone number {} is blacklisted", phoneNumber);
        }
        return blacklisted;
    }
} 