package org.huex.liarbarback.models;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class Player {
    private String userId;
    private String name;
    private boolean isActive;
    private boolean isReady;
    private boolean isHost;
    private String roomId;
    private List<Card> handCards;
    private List<Card> playedCards;

    public Player(String userId) {
        this.userId = userId;
        this.name = "Player_" + userId.substring(0,Math.min(5,userId.length())); // Default name based on userId
        this.isActive = true;
        this.isReady = false;
        this.isHost = false; // Default to not being a host
        handCards = new CopyOnWriteArrayList<>();
        playedCards = new CopyOnWriteArrayList<>();
    }

    @Override
    public String toString() {
        return userId.substring(0,Math.min(5,userId.length()-1))+"\t"
            +name+"\t"
            +(isActive?"Active":"Inactive")+"\t"
            +(isReady?"Ready":"NotReady")+"\t"
            +(isHost?"Host":"NotHost")+"\n";
    }

    public boolean hasCard(Card card) {
        return handCards.stream().anyMatch(handCard -> handCard.equals(card));
    }

    public boolean playCard(Card card) {
        if (!hasCard(card)) return false; 
        handCards.remove(card);
        playedCards.add(card);
        System.out.println(name + " played card: " + card);
        return true;
    }

    public boolean playCards(List<Card> cards) {
        boolean success = Card.moveCards(handCards, playedCards, cards);
        if (!success) {
            System.out.println(name + " failed to play cards: " + cards);
            return false;
        }
        System.out.println(name + " played cards: " + cards);
        return true;
    }

    public void addCard(Card card) {
        handCards.add(card);
    }

    public void restart() {
        isReady = false;
        handCards.clear();
        playedCards.clear();
    }

}
