package com.mostafa.WebSocketdemo.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
//enable our WebSocket server
@EnableWebSocketMessageBroker 
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{

	//register a websocket endpoint that the clients will use to connect to our websocket server.
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		
		//enable fallback options for browsers that don’t support websocket.
        registry.addEndpoint("/ws").withSockJS();
    }

	 //route messages from one client to another.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

    	registry.setApplicationDestinationPrefixes("/app");
    
        registry.enableSimpleBroker("/topic");
    }
}
