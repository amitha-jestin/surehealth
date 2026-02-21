package com.sociolab.surehealth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

        // thread-safe
        private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

        public void blacklistToken(String token) {
            blacklist.add(token);
        }

        public boolean isBlacklisted(String token) {
            return blacklist.contains(token);
        }
}
