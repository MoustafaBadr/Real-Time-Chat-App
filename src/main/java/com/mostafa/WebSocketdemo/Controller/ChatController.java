package com.mostafa.WebSocketdemo.Controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.mostafa.WebSocketdemo.Model.ChatMessage;

@Controller
public class ChatController {

	// receiving messages from one client and then broadcasting it to others
	@MessageMapping("/chat.sendMessage")
	@SendTo("/topic/public")
	public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {

		return chatMessage;

	}

	@MessageMapping("/chat.addUser")
	@SendTo("/topic/public")
	public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {

		// Add username in web soket session
		headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());

		return chatMessage;

	}

}
