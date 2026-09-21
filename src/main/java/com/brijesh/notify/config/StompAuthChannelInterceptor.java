package com.brijesh.notify.config;


import com.brijesh.notify.auth.JwtService;
import com.brijesh.notify.user.User;
import com.brijesh.notify.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;

@Configuration
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")){
                throw new IllegalArgumentException("Missing Authorization header on STOMP CONNECT");
            }

            String email = jwtService.extractEmail(authHeader.substring(7));
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user"));

            // Whatever we set here becomes the routing key for convertAndSendToUser(...) later
            Principal principal = () -> String.valueOf(user.getId());
            accessor.setUser(principal);
        }

        return message;
    }
}
