package com.example.hm1.controller;

import com.example.hm1.dao.UserRepository;
import com.example.hm1.dto.chat.ChatContactResponse;
import com.example.hm1.dto.chat.ChatMessageRequest;
import com.example.hm1.dto.chat.ChatMessageResponse;
import com.example.hm1.dto.chat.ChatThreadResponse;
import com.example.hm1.entity.User;
import com.example.hm1.service.chat.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService,
                          UserRepository userRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/threads")
    public ResponseEntity<List<ChatThreadResponse>> getThreads() {
        User currentUser = resolveCurrentUser();
        List<ChatThreadResponse> threads = chatService.getThreadsForUser(currentUser);
        return ResponseEntity.ok(threads);
    }

    @GetMapping("/threads/{threadId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable Long threadId) {
        User currentUser = resolveCurrentUser();
        List<ChatMessageResponse> messages = chatService.getMessagesForThread(threadId, currentUser);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        User currentUser = resolveCurrentUser();
        request.setSenderUserId(currentUser.getId());
        ChatMessageResponse response = chatService.sendMessage(request);
        messagingTemplate.convertAndSend("/topic/chat/" + response.getThreadId(), response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<ChatContactResponse>> getContacts() {
        User currentUser = resolveCurrentUser();
        List<ChatContactResponse> contacts = userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .map(user -> new ChatContactResponse(user.getId(), user.getUsername()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/me")
    public ResponseEntity<ChatContactResponse> getCurrentUserProfile() {
        User currentUser = resolveCurrentUser();
        return ResponseEntity.ok(new ChatContactResponse(currentUser.getId(), currentUser.getUsername()));
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            throw new IllegalStateException("Cannot resolve current user");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
}

