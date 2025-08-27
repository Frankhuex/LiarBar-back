package org.huex.liarbarback;

import org.huex.liarbarback.events.RoomUpdatedEvent;
import org.huex.liarbarback.managers.PlayerManager;
import org.huex.liarbarback.managers.RoomManager;
import org.huex.liarbarback.managers.SessionManager;
import org.huex.liarbarback.models.Message;
import org.huex.liarbarback.models.Message.MsgType;
import org.huex.liarbarback.models.PlayCards;
import org.huex.liarbarback.models.Player;
import org.huex.liarbarback.models.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.websocket.Session;

@Component
public class MsgHandler {
    @Autowired RoomManager roomManager;
    @Autowired PlayerManager playerManager;
    @Autowired SessionManager sessionManager;
    @Autowired private ApplicationEventPublisher eventPublisher;
    
    public boolean handleMsg(Message<?> message, Session session, String userId) {
        try {
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
                case START_GAME -> {
                    return handleStartGame(session, userId);
                }
                case PLAY_CARDS,SKIP,CHALLENGE -> {
                    return handlePerformOperation(session, userId, message.getMsgType(), (PlayCards)message.getData());
                }
                case RESTART -> {
                    return handleRestartGame(session, userId);
                }
                default -> {
                    System.err.println("Unknown message type: "+message.getMsgType());
                    session.getAsyncRemote().sendObject(new Message<>(MsgType.ERROR, "Unsupported message type: "+message.getMsgType()));
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
            System.err.println(e.getStackTrace());
            session.getAsyncRemote().sendObject(new Message<>(MsgType.ERROR, "Failed to handle message: "+e.getMessage()));
            return false;
        }
    }

    @EventListener
    public void roomUpdatedListener(RoomUpdatedEvent event) {
        Room room = roomManager.getRoom(event.getRoomId()).orElse(null);
        if (room==null) return;
        if (room.getPlayerList().isEmpty()) {
            roomManager.removeRoom(room.getId());
            System.out.println("Room " + room.getId() + " removed due to no players.");
            return;
        }
        broadcastRoom(room);
    }

    public void broadcastRoom(Room room) {
        System.out.println("Start broadcasting room");
        System.out.println(room);
        for (Player p : room.getPlayerList()) {
            if (p.isActive()) {
                Session session = sessionManager.getSession(p.getUserId()).orElse(null);
                if (session==null) {
                    p.setActive(false);
                    continue;
                }
                Message<Room> message = new Message<>(Message.MsgType.ROOM_PLAYERS_LIST, room);
                session.getAsyncRemote().sendObject(message);
                System.out.println("Sent msg to player "+p.getName()+" MsgID:"+message.getMsgId());
            }
        }
        System.out.println("End broadcasting room");
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
            System.out.println("Room " + room.getId() + " created by user: " + userId);
            broadcastRoom(room);
            return true;
        } catch (Exception e) {
            System.err.println("Error creating room: " + e.getMessage());
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ERROR, "Failed to create room"));
            e.printStackTrace();
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
        if (room.getPlayerList().isEmpty()) {
            eventPublisher.publishEvent(new RoomUpdatedEvent(this, player.getRoomId()));
        } else {
            broadcastRoom(room);
        }
        System.out.println("Player " + userId + " left room "+room.getId());
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
        System.out.println("Player " + userId + " changed name to " + name);
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
        System.out.println("Player " + userId + (isReady?" ready":" not ready"));
        broadcastRoom(room);
        return true;
    }

    public boolean handleStartGame(Session session, String userId) {
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
        if (!player.isHost()) {
            System.err.println("Player " + userId + " is not the host");
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ERROR, "You are not the host"));
            return false;
        }
        if (!room.isStarted() && room.getPlayerList().stream().allMatch(Player::isReady)) {
            if (room.startGame()) {
                broadcastRoom(room);
                System.out.println("Game started: "+room);
                return true;
            }
            return false;
        } else {
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ERROR, "Game cannot be started"));
            return false;
        }
    }

    public boolean handleRestartGame(Session session, String userId) {
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
        if (!player.isHost()) {
            System.err.println("Player " + userId + " is not the host");
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ERROR, "You are not the host"));
            return false;
        }
        room.restartGame();
        broadcastRoom(room);
        return true;
    }

    public boolean handlePerformOperation(Session session, String userId, MsgType msgType, PlayCards playCards) {
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
        if (!room.isStarted()) {
            System.err.println("Game not started in room " + room.getId());
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ERROR, "Game not started"));
            return false;
        }
        ///////////////////////////////////////////////////////////

        boolean success;
        switch (msgType) {
            case PLAY_CARDS -> {
                success=room.playCards(playCards, userId);
            }
            case SKIP -> {
                success=room.skip(userId);
            }
            case CHALLENGE -> {
                success=room.challenge(userId);
            }
            default -> {
                System.err.println("Unsupported operation: " + msgType);
                session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ERROR, "Unsupported operation"));
                return false;
            }
        }

        ///////////////////////////////////////////////////////////
        broadcastRoom(room);
        if (!success) {
            System.err.println("Player " + player.getName() + " failed to play cards");
            session.getAsyncRemote().sendObject(new Message<>(Message.MsgType.ERROR, "Failed to play cards"));
            return false;
        }
        return true;
    }

}
