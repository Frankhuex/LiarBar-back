package org.huex.liarbarback.models;
import lombok.*;

@Getter @Setter @AllArgsConstructor
public class Player {
    private String userId;
    private String name;
    private boolean isActive;
    private boolean isReady;
    private boolean isHost;
    private String roomId;

    public Player(String userId) {
        this.userId = userId;
        this.name = "Player_" + userId.substring(0, 5); // Default name based on userId
        this.isActive = true;
        this.isReady = false;
        this.isHost = false; // Default to not being a host
    }

    @Override
    public String toString() {
        return userId.substring(0,5)+"\t"
            +name+"\t"
            +(isActive?"Active":"Inactive")+"\t"
            +(isReady?"Ready":"NotReady")+"\t"
            +(isHost?"Host":"NotHost")+"\n";
    }
}
