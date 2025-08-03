package org.huex.liarbarback.managers;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.Session;

import org.huex.liarbarback.models.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SessionManager {
    @Autowired private PlayerManager playerManager;
    // 使用线程安全的Map存储所有连接
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    /**
     * 添加连接
     */
    public void addSession(String userId, Session session) {
        sessions.put(userId, session);
        System.out.println("User " + userId + " connected. Total connections: " + sessions.size());
    }
    
    /**
     * 移除连接
     */
    public boolean removeSession(String userId) {
        Player player=playerManager.getPlayer(userId).orElse(null);
        if(player==null) return false;
        player.setActive(false);
        sessions.remove(userId);
        System.out.println("User " + userId + " disconnected. Total connections: " + sessions.size());
        return true;
    }
    
    /**
     * 获取特定用户的会话
     */
    public Optional<Session> getSession(String userId) {
        return Optional.ofNullable(sessions.get(userId));
    }
    
    /**
     * 获取所有会话
     */
    public Set<Map.Entry<String, Session>> getAllSessions() {
        return sessions.entrySet();
    }
    
    /**
     * 获取连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }
    
    /**
     * 广播消息给所有用户
     */
    public void broadcast(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    System.err.println("Error sending message to user: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 发送消息给特定用户
     */
    public boolean sendMessageToUser(String userId, String message) {
        Session session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getAsyncRemote().sendText(message);
                return true;
            } catch (Exception e) {
                System.err.println("Error sending message to user " + userId + ": " + e.getMessage());
                return false;
            }
        }
        return false;
    }
}
