package org.huex.liarbarback.models;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.huex.liarbarback.models.Card.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class Room {
    private String id;
    private List<Player> playerList;
    private int maxPlayers;

    private boolean isStarted;
    private boolean isEnded;

    private List<Card> cardDeck;
    private int currentPlayerIndex;
    private int roundBeginnerIndex;
    private Card.Rank currentClaimRank;
    private Player winner;


    

    public Room(String id) {
        this.id=id;
        playerList = new CopyOnWriteArrayList<>();
        maxPlayers = 8;

        isStarted = false;
        isEnded = false;

        cardDeck = null;
        currentPlayerIndex = -1;
        roundBeginnerIndex = -1;
        currentClaimRank = Rank.NULL;

        winner = null;
    }

    public Player getPlayer(String userId) {
        for (Player player : playerList) {
            if (player.getUserId().equals(userId)) {
                return player;
            }
        }
        return null; 
    }

    public boolean addPlayer(Player player) {
        if (isFull()) {
            return false;
        }
        playerList.add(player);
        System.out.println("Player " + player.getName() + " joined room "+id);
        return true;
    }

    public boolean removePlayer(String userId) {
        Player player = getPlayer(userId);
        if (player != null) {
            if (player.isHost() && playerList.size() > 1) {
                playerList.get(1).setHost(true);
            }
            playerList.remove(player);
            System.out.println("Player " + userId + " removed from room "+ id);
            return true;
        }
        return false;
    }

    public boolean isFull() {
        return playerList.size() >= maxPlayers;
    }

    public boolean startGame() {
        if (isStarted) {
            System.out.println("Game already started");
            return false;
        }

        isStarted = true;
        isEnded = false;

        // Deal cards to players
        cardDeck = new CopyOnWriteArrayList<>();
        for (Suit suit : Suit.values()) {
            if (suit==Suit.UNKNOWN) continue; // Skip UNKNOWN suit
            for (Rank rank : Rank.values()) {
                if (rank==Rank.NULL) continue;
                cardDeck.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(cardDeck);
        Collections.shuffle(playerList);
        // n*m+2+k=52 && 2+k>=8
        int cardsPerPlayer = 52/playerList.size();
        int minOutCards = 8; // Can change later
        while (52 - cardsPerPlayer*playerList.size() <= minOutCards) {
            cardsPerPlayer--;
        }
        for (int i=0;i<playerList.size();i++) {
            Player player = playerList.get(i);
            for (int j=0;j<cardsPerPlayer;j++) {
                player.addCard(cardDeck.remove(0));
            }
        }
        // First player gets 2 more cards
        playerList.get(0).addCard(cardDeck.remove(0));
        playerList.get(0).addCard(cardDeck.remove(0));

        currentPlayerIndex = 0;
        roundBeginnerIndex = 0;

        System.out.println("Game started: "+this);
        return true;
    }

    public void restartGame() {
        isStarted = false;
        isEnded = false;
        cardDeck = null;
        currentPlayerIndex = 0;
        roundBeginnerIndex = 0;
        currentClaimRank = Rank.NULL;

        for (Player player : playerList) {
            if (!player.isActive()) {
                removePlayer(player.getUserId());
                continue;
            }
            player.restart();
        }
        System.out.println("Game restarted: "+this);

    }



    private boolean isTurn(String userId) {
        return playerList.get(currentPlayerIndex).getUserId().equals(userId);
    }

    public boolean playCards(PlayCards playCards, String userId) {
        if (!isTurn(userId)) {
            System.out.println("It's not " + userId + "'s turn.");
            return false;
        }
        if (currentClaimRank!=Rank.NULL && !playCards.getClaimRank().equals(currentClaimRank)) {
            System.out.println("Invalid claim rank. Current: " + currentClaimRank + ", get: " + playCards.getClaimRank());
            return false;
        }
        if (currentClaimRank==Rank.NULL) {
            currentClaimRank = playCards.getClaimRank();
            roundBeginnerIndex = currentPlayerIndex;
        }
        Player player = playerList.get(currentPlayerIndex);
        boolean success = player.playCards(playCards.cards);
        if (!success) {
            System.out.println("Player " + userId + " failed to play cards.");
            return false;
        }
        System.out.println("Player " + userId + " played cards: " + playCards.cards);
        System.out.println("Player " + userId + " has " + player.getHandCards().size() + " cards left.");
        
        currentPlayerIndex = (currentPlayerIndex + 1) % playerList.size();

        if (!playerList.get(currentPlayerIndex).isActive()) {
            skip(playerList.get(currentPlayerIndex).getUserId());
        }

        return true;
    }

    public boolean skip(String userId) {
        if (!isTurn(userId)) {
            System.out.println("It's not " + userId + "'s turn.");
            return false;
        }
        if (currentClaimRank == Rank.NULL) {
            System.out.println("You are beginner. Cannot skip.");
            return false;
        }
        if (currentPlayerIndex == roundBeginnerIndex) {
            Player currentPlayer = playerList.get(currentPlayerIndex);
            if (currentPlayer.getHandCards().isEmpty()) {
                isEnded = true;
                winner = currentPlayer;
                System.out.println("Player " + currentPlayer.getName() + " wins the game!");
                return true;
            }
            currentClaimRank = Rank.NULL; // New round
            System.out.println("New round begins.");
            for (Player player : playerList) {
                Card.moveAllCards(player.getPlayedCards(), cardDeck);
            }
        } else {
            currentPlayerIndex = (currentPlayerIndex + 1) % playerList.size();
            if (!playerList.get(currentPlayerIndex).isActive()) {
                skip(playerList.get(currentPlayerIndex).getUserId());
            }
        }
        System.out.println("Player " + userId + " skipped.");
        return true;
    }

    public boolean challenge(String userId) {
        if (!isTurn(userId)) {
            System.out.println("It's not " + userId + "'s turn.");
            return false;
        }
        if (currentClaimRank == Rank.NULL) {
            System.out.println("You are beginner. No claim to challenge.");
            return false;
        }
        Player lastPlayer = playerList.get((currentPlayerIndex-1+playerList.size())%playerList.size());
        Player currentPlayer = playerList.get(currentPlayerIndex);
        for (Card card: lastPlayer.getPlayedCards()) {
            if (!card.getRank().equals(currentClaimRank)) {
                System.out.println("Challenge success! " + lastPlayer.getName() + " lied.");
                System.out.println("Played: "+card.getRank()+", Claimed: "+currentClaimRank);
                for (Player player : playerList) {
                    Card.moveAllCards(player.getPlayedCards(),lastPlayer.getHandCards());
                }
                roundBeginnerIndex = currentPlayerIndex;
                currentClaimRank = Rank.NULL;
                return true;
            }
        }
        System.out.println("Challenge failed! " + lastPlayer.getName() + " didn't lie.");
        for (Player player : playerList) {
            Card.moveAllCards(player.getPlayedCards(),currentPlayer.getHandCards());
        }
        currentPlayerIndex = (currentPlayerIndex-1+playerList.size())%playerList.size();
        roundBeginnerIndex = currentPlayerIndex;
        currentClaimRank = Rank.NULL;

        if (!playerList.get(currentPlayerIndex).isActive()) {
            skip(playerList.get(currentPlayerIndex).getUserId());
        }
        return true;
    }














    @Override
    public String toString() {
        String str = 
            "--------------------------------------------------------------------\n"
            + "Room " +id + "\n"
            + "--------------------------------------------------------------------\n"
            + "ID\tName       \tActive\tReady\tHost\tHand\tPlayed\n"
            + "--------------------------------------------------------------------\n";
        for (Player player : playerList) {
            str += player.toString();
        }
        str+="---------------------------------------------------------------------\n"
        + "Max players: " + maxPlayers + "\n"
        + "Started: " + isStarted + "\n"
        + "Ended: " + isEnded + "\n"
        + "Card deck: " + (cardDeck==null? "null" : cardDeck.size()) + "\n"
        + "Current player: " + currentPlayerIndex + "\n"
        + "Round beginner: " + roundBeginnerIndex + "\n"
        + "Current claim rank: " + currentClaimRank + "\n"
        + "Winner: " + (winner==null? "null" : winner.getName()) + "\n"
        + "---------------------------------------------------------------------\n";
        return str;
    }




    
}
