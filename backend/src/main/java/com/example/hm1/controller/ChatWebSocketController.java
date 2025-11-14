package com.example.hm1.controller;

import com.example.hm1.dto.chat.ChatMessageRequest;
import com.example.hm1.dto.chat.ChatMessageResponse;
import com.example.hm1.service.chat.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/send")
    public void handleChatMessage(ChatMessageRequest request) {
        ChatMessageResponse response = chatService.sendMessage(request);
        messagingTemplate.convertAndSend("/topic/chat/" + response.getThreadId(), response);
    }
}

