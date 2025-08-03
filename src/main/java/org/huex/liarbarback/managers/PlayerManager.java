package org.huex.liarbarback.managers;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.huex.liarbarback.models.Player;
import org.huex.liarbarback.models.Room;
import org.springframework.stereotype.Component;

@Component
public class PlayerManager {
    private final Map<String, Player> players = new ConcurrentHashMap<>();

    public void addPlayer(Player player) {
        players.put(player.getUserId(), player);
    }

    public Optional<Player> getPlayer(String userId) {
        return Optional.ofNullable(players.get(userId));
    }
    
    public boolean removePlayer(String userId) {
        return players.remove(userId)!= null;
    }
}
