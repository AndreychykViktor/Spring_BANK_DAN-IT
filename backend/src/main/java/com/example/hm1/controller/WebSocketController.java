package com.example.hm1.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class WebSocketController {

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Map<String, Object> greeting(Map<String, Object> message) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", "Hello, " + message.get("name") + "!");
        response.put("timestamp", LocalDateTime.now());
        response.put("type", "greeting");
        
        return response;
    }

    @MessageMapping("/account/subscribe")
    @SendTo("/topic/account-subscriptions")
    public Map<String, Object> subscribeToAccount(Map<String, Object> subscription) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "subscribed");
        response.put("accountNumber", subscription.get("accountNumber"));
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Successfully subscribed to account updates");
        
        return response;
    }
}
