# Real-Time-Chat-App
Spring Boot WebSocket Chat Demo 
In this article, you’ll learn how to use WebSocket API with Spring Boot and build a simple group chat application at the end.

WebSocket is a communication protocol that makes it possible to establish a two-way communication channel between a server and a client.

WebSocket works by first establishing a regular HTTP connection with the server and then upgrading it to a bidirectional websocket connection by sending an Upgrade header.

WebSocket is supported in most modern web browsers and for browsers that don’t support it, we have libraries that provide fallbacks to other techniques like comet and long-polling.

WebSocket Configuration
The first step is to configure the websocket endpoint and message broker. Create a new package config inside com.example.websocketdemo package, then create a new class WebSocketConfig inside config package with the following contents -

package com.example.websocketdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }
}

The @EnableWebSocketMessageBroker is used to enable our WebSocket server. We implement WebSocketMessageBrokerConfigurer interface and provide implementation for some of its methods to configure the websocket connection.

In the first method, we register a websocket endpoint that the clients will use to connect to our websocket server.

Notice the use of withSockJS() with the endpoint configuration. SockJS is used to enable fallback options for browsers that don’t support websocket.Under the hood SockJS tries to use native WebSockets first. If that fails it can use a variety of browser-specific transport protocols and presents them through WebSocket-like abstractions.

SockJS is intended to work for all modern browsers and in environments which don't support the WebSocket protocol

You might have noticed the word STOMP in the method name. These methods come from Spring frameworks STOMP implementation. STOMP stands for Simple Text Oriented Messaging Protocol. It is a messaging protocol that defines the format and rules for data exchange.

Why do we need STOMP? Well, WebSocket is just a communication protocol. It doesn’t define things like - How to send a message only to users who are subscribed to a particular topic, or how to send a message to a particular user. We need STOMP for these functionalities.

In the second method, we’re configuring a message broker that will be used to route messages from one client to another.

The first line defines that the messages whose destination starts with “/app” should be routed to message-handling methods (we’ll define these methods shortly).

And, the second line defines that the messages whose destination starts with “/topic” should be routed to the message broker. Message broker broadcasts messages to all the connected clients who are subscribed to a particular topic.

In the above example, We have enabled a simple in-memory message broker. But you’re free to use any other full-featured message broker like RabbitMQ or ActiveMQ.
-------------------------------------------------------------------
Creating the ChatMessage model
ChatMessage model is the message payload that will be exchanged between the clients and the server. Create a new package model inside com.example.websocketdemo package, and then create the ChatMessage class inside model package with the following contents -

package com.example.websocketdemo.model;

public class ChatMessage {
    private MessageType type;
    private String content;
    private String sender;
------------------------
//Java Enum can be thought of as classes that have fixed set of constants.
 Enum in java is a data type that contains fixed set of constants.
>>Points to remember for Java Enum
  enum improves type safety
  enum can be easily used in switch
  enum can be traversed
  enum can have fields, constructors and methods
  enum may implement many interfaces but cannot extend any class because it internally extends Enum class.
------------------------
    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
-----------------------------------------------------------------------
Creating the Controller for sending and receiving messages
We’ll define the message handling methods in our controller. These methods will be responsible for receiving messages from one client and then broadcasting it to others.

Create a new package controller inside the base package and then create the ChatController class with the following contents -

package com.example.websocketdemo.controller;

import com.example.websocketdemo.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, 
                               SimpMessageHeaderAccessor headerAccessor) {
        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        return chatMessage;
    }

}
If you recall from the websocket configuration, all the messages sent from clients with a destination starting with /app will be routed to these message handling methods annotated with @MessageMapping.

For example, a message with destination /app/chat.sendMessage will be routed to the sendMessage() method, and a message with destination /app/chat.addUser will be routed to the addUser() method.
---------------------------
Adding WebSocket Event listeners
We’ll use event listeners to listen for socket connect and disconnect events so that we can log these events and also broadcast them when a user joins or leaves the chat room -

package com.example.websocketdemo.controller;

import com.example.websocketdemo.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        logger.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if(username != null) {
            logger.info("User Disconnected : " + username);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(ChatMessage.MessageType.LEAVE);
            chatMessage.setSender(username);

            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        }
    }
}
We’re already broadcasting user join event in the addUser() method defined inside ChatController. So, we don’t need to do anything in the SessionConnected event.

In the SessionDisconnect event, we’ve written code to extract the user’s name from the websocket session and broadcast a user leave event to all the connected clients.
---------------------------------------------------
Using RabbitMQ as the message broker
If you want to use a full featured message broker like RabbitMQ instead of the simple in-memory message broker then just add the following dependencies in your pom.xml file -

<!-- RabbitMQ Starter Dependency -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Following additional dependency is required for Full Featured STOMP Broker Relay -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-reactor-netty</artifactId>
</dependency>
Once you’ve added the above dependencies, you can enable RabbitMQ message broker in the WebSocketConfig.java file like this -

public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app");

    // Use this for enabling a Full featured broker like RabbitMQ
    registry.enableStompBrokerRelay("/topic")
            .setRelayHost("localhost")
            .setRelayPort(61613)
            .setClientLogin("guest")
            .setClientPasscode("guest");
