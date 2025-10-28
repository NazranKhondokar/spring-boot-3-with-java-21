package com.nazran.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Interceptor for WebSocket subscriptions.
 * Logs and validates subscription attempts.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketSubscriptionInterceptor implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    StompCommand command = accessor.getCommand();

                    if (StompCommand.SUBSCRIBE.equals(command)) {
                        String destination = accessor.getDestination();
                        String sessionId = accessor.getSessionId();

                        log.info("User subscribing to: {} (Session: {})", destination, sessionId);

                        // Validate subscription destination
                        if (destination != null && !isValidDestination(destination)) {
                            log.warn("Invalid subscription destination: {}", destination);
                            throw new IllegalArgumentException("Invalid subscription destination");
                        }
                    }

                    if (StompCommand.UNSUBSCRIBE.equals(command)) {
                        String subscriptionId = accessor.getSubscriptionId();
                        log.info("User unsubscribed: {}", subscriptionId);
                    }
                }

                return message;
            }
        });
    }

    private boolean isValidDestination(String destination) {
        // Validate allowed subscription patterns
        return destination.startsWith("/topic/") ||
                destination.startsWith("/queue/") ||
                destination.startsWith("/user/");
    }
}