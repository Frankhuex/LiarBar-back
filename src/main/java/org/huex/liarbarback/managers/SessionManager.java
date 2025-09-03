package org.huex.liarbarback.managers;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.huex.liarbarback.MsgHandler;

import jakarta.websocket.Session;

import org.huex.liarbarback.WebSocketServer;
import org.huex.liarbarback.events.RoomUpdatedEvent;
import org.huex.liarbarback.models.Player;
import org.huex.liarbarback.models.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SessionManager {
    @Autowired private PlayerManager playerManager;
    @Autowired private RoomManager roomManager;
    @Autowired private ApplicationEventPublisher eventPublisher;

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
        Room room=roomManager.getRoom(player.getRoomId()).orElse(null);

        if (room!=null) {
            if (!room.isStarted()) {
                playerManager.removePlayer(userId);
                System.out.println("Player removed: " + userId);
                room.removePlayer(userId);
                System.out.println("Player removed from room: " + userId);
                
            } else {
                // 如果房间已经开始，玩家对象不删除，标记为不活跃
                player.setActive(false);
                System.out.println("Player marked as inactive: " + userId);
                if (userId.equals(room.getPlayerList().get(room.getCurrentPlayerIndex()).getUserId())) {
                    room.autoPlay(userId);
                    System.out.println("Player autoplayed: " + userId);
                }
            }
        } else {
            // 如果房间不存在，直接删除玩家
            playerManager.removePlayer(userId);
            System.out.println("Room not found, player removed: " + userId);
        }
        eventPublisher.publishEvent(new RoomUpdatedEvent(this, player.getRoomId()));
        sessions.remove(userId);
        System.out.println("Session for user " + userId + " disconnected. Total connections: " + sessions.size());
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
