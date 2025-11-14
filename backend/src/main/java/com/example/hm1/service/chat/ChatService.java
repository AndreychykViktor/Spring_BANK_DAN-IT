package com.example.hm1.service.chat;

import com.example.hm1.dto.chat.ChatMessageRequest;
import com.example.hm1.dto.chat.ChatMessageResponse;
import com.example.hm1.dto.chat.ChatThreadResponse;
import com.example.hm1.entity.User;

import java.util.List;

public interface ChatService {

    ChatThreadResponse getOrCreateThread(User currentUser, User otherUser);

    List<ChatThreadResponse> getThreadsForUser(User user);

    List<ChatMessageResponse> getMessagesForThread(Long threadId, User user);

    ChatMessageResponse sendMessage(ChatMessageRequest request);
}

