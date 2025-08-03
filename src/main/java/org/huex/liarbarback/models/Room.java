package org.huex.liarbarback.models;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class Room {
    private String id;
    private List<Player> playerList;
    private int maxPlayers;
    private boolean isStarted;

    public Room(String id) {
        this.id=id;
        playerList = new CopyOnWriteArrayList<>();
        maxPlayers = 8;
    }

    public Player getPlayer(String userId) {
        for (Player player : playerList) {
            if (player.getUserId().equals(userId)) {
                return player;
            }
        }
        return null; 
    }

    public void addPlayer(Player player) {
        playerList.add(player);
    }

    public boolean removePlayer(String userId) {
        Player player = getPlayer(userId);
        if (player != null) {
            playerList.remove(player);
            return true;
        }
        return false;
    }

    public boolean isFull() {
        return playerList.size() >= maxPlayers;
    }

    @Override
    public String toString() {
        String str = "Room " +id + "\n"
            + "ID\tName\tStatus\tReady\tHost\n"
            + "--------------------------------------------\n";
        for (Player player : playerList) {
            str += player.toString();
        }
        str+="--------------------------------------------\n";
        return str;
    }




    
}
