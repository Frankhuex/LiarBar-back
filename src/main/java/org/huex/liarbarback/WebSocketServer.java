package org.huex.liarbarback;


import java.io.IOException;

import org.huex.liarbarback.managers.SessionManager;
import org.huex.liarbarback.models.Message;
import org.huex.liarbarback.models.MessageEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(
    value = "/api/ws/{user_id}",
    configurator = SpringEndpointConfigurator.class, // 使用自定义配置器
    encoders = {MessageEncoder.class}
)
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) // 设置为原型作用域
public class WebSocketServer {
    private String userId;


    private SessionManager sessionManager;
    private MsgHandler msgHandler;
    @Autowired
    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Autowired
    public void setMsgHandler(MsgHandler msgHandler) {
        this.msgHandler = msgHandler;
    }


    @OnOpen
    public void onOpen(Session session, @PathParam("user_id") String userId) {
        this.userId = userId;
        sessionManager.addSession(userId, session);
        System.out.println("WebSocket connection opened for user: " + userId);
        session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.WELCOME, "Welcome to the WebSocket server!"));
    }

    @OnClose
    public void onClose(Session session, @PathParam("user_id") String userId) {
        sessionManager.removeSession(userId);
        System.out.println("WebSocket connection closed for user: " + userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // System.out.println("Received message from user: " + userId + " message: " + message);
        // session.getAsyncRemote().sendText("Hello from server");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            Message<?> receivedMessage = mapper.readValue(message, Message.class);
            boolean success=msgHandler.handleMsg(receivedMessage, session, userId);
            System.out.println("Message handled: " + receivedMessage + (success?" success":" failed"));
        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
            session.getAsyncRemote().sendText("Error parsing message: " + e.getMessage());
        }
        
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
        try {
            sessionManager.removeSession(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
