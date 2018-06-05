package com.mostafa.WebSocketdemo.Controller;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import com.mostafa.WebSocketdemo.Model.ChatMessage;

public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    	logger.info("Received a new web socket connection");
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionConnectedEvent event) {
    	
    	StompHeaderAccessor headerAccessor =StompHeaderAccessor.wrap(event.getMessage());
    	
    	String username = (String)headerAccessor.getSessionAttributes().get("username");
    	if(username != null) {
    		
    		logger.info("user Disconnected : "+ username);
    		
    		ChatMessage chatmessage = new ChatMessage();
    		chatmessage.setType(ChatMessage.MessageType.LEAVE);
    		chatmessage.setSender(username);

            messagingTemplate.convertAndSend("/topic/public", chatmessage);
    	}
    }

}
