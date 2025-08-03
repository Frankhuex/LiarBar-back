package org.huex.liarbarback.managers;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.huex.liarbarback.models.Player;
import org.huex.liarbarback.models.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoomManager {
    @Autowired private PlayerManager playerManager;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Room createRoom(String userId) {
        String roomId = generateRoomId();
        Room room = new Room(roomId);
        Player player=new Player(userId);
        player.setHost(true);
        player.setRoomId(roomId);
        playerManager.addPlayer(player);
        room.addPlayer(player);
        rooms.put(roomId, room);
        System.out.println("Room created with ID: " + roomId + " by user: " + userId);
        return room;
    }

    public Optional<Room> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }


    public String generateRoomId() {
        StringBuilder roomId;
        do {
            String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            // 只取前6位，并确保只包含字母和数字
            roomId = new StringBuilder();
            String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            
            for (int i = 0; i < 6; i++) {
                char c = uuid.charAt(i);
                if (allowedChars.indexOf(c) >= 0) {
                    roomId.append(c);
                } else {
                    // 如果不是允许的字符，随机选择一个
                    roomId.append(allowedChars.charAt((int)(Math.random() * allowedChars.length())));
                }
            }
        } while (rooms.containsKey(roomId.toString()));
        return roomId.toString();
    }
}
