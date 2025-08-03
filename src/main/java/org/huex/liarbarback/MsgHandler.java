package org.huex.liarbarback;

import org.huex.liarbarback.managers.PlayerManager;
import org.huex.liarbarback.managers.RoomManager;
import org.huex.liarbarback.managers.SessionManager;
import org.huex.liarbarback.models.Message;
import org.huex.liarbarback.models.Player;
import org.huex.liarbarback.models.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.websocket.Session;

@Component
public class MsgHandler {
    @Autowired RoomManager roomManager;
    @Autowired PlayerManager playerManager;
    @Autowired SessionManager sessionManager;
    
    public boolean handleMsg(Message<?> message, Session session, String userId) {
        switch (message.getMsgType()) {
            case CREATE_ROOM -> {
                return handleCreateRoom(session, userId);
            }
            case JOIN_ROOM -> {
                return handleJoinRoom(session, userId, message.getData().toString());
            }
            case LEAVE_ROOM -> {
                return handleLeaveRoom(session, userId);
            }
            case CHANGE_NAME -> {
                return handleChangeName(session, userId, message.getData().toString());
            }
            case GET_ROOM_PLAYERS -> {
                return sendRoomPlayers(session, message.getData().toString());
            }
            case PREPARE -> {
                return handlePrepare(session, userId, (boolean)message.getData());
            }
        }
        return false;
    }

    public void broadcastRoom(Room room) {
        System.out.println(room);
        for (Player p : room.getPlayerList()) {
            if (p.isActive()) {
                Session session = sessionManager.getSession(p.getUserId()).orElse(null);
                if (session==null) {
                    p.setActive(false);
                    continue;
                }
                session.getAsyncRemote()
                    .sendObject(new Message<>(Message.MsgType.ROOM_PLAYERS_LIST, room));

            }
        }
    }


    public boolean checkPlayerInRoom(Player player, Room room) {
        if (player.getRoomId()==null
            || !player.getRoomId().equals(room.getId())
            || !room.getPlayerList().contains(player)
        ) {
            room.removePlayer(player.getUserId());
            playerManager.removePlayer(player.getUserId());
            return false;
        }
        return true;
    }


    public boolean handleCreateRoom(Session session, String userId) {
        if (playerManager.getPlayer(userId).isPresent()) {
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ALREADY_IN_ROOM, "Already in a room"));
            System.err.println("Player " + userId + " already in a room");
            return false;
        }
        try {
            Room room = roomManager.createRoom(userId);
            broadcastRoom(room);
            return true;
        } catch (Exception e) {
            System.err.println("Error creating room: " + e.getMessage());
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ERROR, "Failed to create room"));
            return false;
        }
    }

    public boolean handleJoinRoom(Session session, String userId, String roomId) {
        try { 
            Room room = roomManager.getRoom(roomId).orElse(null);
            if (room == null) {
                session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ROOM_NOT_FOUND, "Room not found"));
                System.err.println("Room " + roomId + " not found");
                return false;
            }
            if (room.isFull()) {
                System.err.println("Room " + roomId + " is full");
                session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ERROR, "Room is full"));
                return false;   
            }
            if (room.isStarted()) {
                System.err.println("Game already started in room " + roomId);
                session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.GAME_ALREADY_STARTED, "Game already started"));
                return false;
            }
            if (!playerManager.getPlayer(userId).isPresent()) {
                Player player=new Player(userId);
                player.setRoomId(roomId);
                room.addPlayer(player);
                playerManager.addPlayer(player);
            }
            broadcastRoom(room);
            return true;
        } catch (Exception e) {
            System.err.println("Error joining room: " + e.getMessage());
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ERROR, "Failed to join room"));
            return false;
        }
    }

    public boolean handleLeaveRoom(Session session, String userId) {
        Player player=playerManager.getPlayer(userId).orElse(null);
        if (player==null) {
            System.err.println("Player " + userId + " not found");
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.PLAYER_NOT_FOUND, "Player not found"));
            return false;
        }
        Room room=roomManager.getRoom(player.getRoomId()).orElse(null);
        if (room==null) {
            System.err.println("Player " + userId + " not in a room");
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ROOM_NOT_FOUND, "Room not found"));
            return false;
        }
        if (room.isStarted()) {
            player.setActive(false);
        } else {
            room.removePlayer(userId);
            playerManager.removePlayer(userId);
        }
        session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ROOM_LEFT, "Left room"));
        broadcastRoom(room);
        return true;
    }

    public boolean handleChangeName(Session session, String userId, String name) {      
        Player player=playerManager.getPlayer(userId).orElse(null);
        if (player==null) {
            System.err.println("Player " + userId + " not found");
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.PLAYER_NOT_FOUND, "Player not found"));
            return false;
        }
        Room room=roomManager.getRoom(player.getRoomId()).orElse(null);
        if (!checkPlayerInRoom(player, room)) {
            System.err.println("Player " + userId + " not in a room");
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.PLAYER_NOT_FOUND, "Player not found in room"));
            return false;
        }
        player.setName(name);
        broadcastRoom(room);
        return true;
    }

    public boolean sendRoomPlayers(Session session, String roomId) {
        Room room=roomManager.getRoom(roomId).orElse(null);
        if (room==null) {
            System.err.println("Room " + roomId + " not found");
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ROOM_NOT_FOUND, "Room not found"));
            return false;
        }
        session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ROOM_PLAYERS_LIST, room));
        return true;
    }

    public boolean handlePrepare(Session session, String userId, boolean isReady) {
        Player player=playerManager.getPlayer(userId).orElse(null);
        if (player==null) {
            System.err.println("Player " + userId + " not found");
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.PLAYER_NOT_FOUND, "Player not found"));
            return false;
        }
        player.setReady(isReady);
        Room room=roomManager.getRoom(player.getRoomId()).orElse(null);
        if (!checkPlayerInRoom(player, room)) {
            System.err.println("Player " + userId + " not in a room");
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.PLAYER_NOT_FOUND, "Player not found in room"));
            return false;
        }
        broadcastRoom(room);
        return true;
    }
}
