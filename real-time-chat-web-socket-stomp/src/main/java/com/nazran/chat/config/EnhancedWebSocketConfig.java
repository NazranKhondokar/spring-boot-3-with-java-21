package com.nazran.chat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Enhanced WebSocket configuration with heartbeat and connection tuning.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class EnhancedWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Create task scheduler for heartbeat
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-");
        taskScheduler.initialize();

        // Enable simple broker with heartbeat
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000}) // 10 seconds
                .setTaskScheduler(taskScheduler);

        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");

        log.info("Enhanced message broker configured with heartbeat");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(
                        "http://localhost:3000",
                        "http://localhost:3001"
                )
                .withSockJS()
                .setStreamBytesLimit(512 * 1024) // 512KB
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000); // 30 seconds

        log.info("STOMP endpoints registered with SockJS fallback");
    }
}