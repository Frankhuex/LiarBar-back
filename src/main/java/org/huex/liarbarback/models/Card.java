package org.huex.liarbarback.models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;


@Getter @AllArgsConstructor @NoArgsConstructor
public class Card {
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    public enum Suit {
        HEARTS, DIAMONDS, CLUBS, SPADES, UNKNOWN
        // 红桃，方块，梅花，黑桃
    }
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    public enum Rank {
        NULL, ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING
    }
    private Suit suit;
    private Rank rank;

    @Override
    public String toString()
    {
        String suitName = suit.toString().substring(0, 1) + suit.toString().substring(1).toLowerCase();
        String rankName = null;
        switch (rank) {
            case NULL:
                rankName = "null";
                break;
            case ACE:
                rankName = "A";
                break;
            case TWO:
                rankName = "2";
                break;
            case THREE:
                rankName = "3";
                break;
            case FOUR:
                rankName = "4";
                break;
            case FIVE:
                rankName = "5";
                break;
            case SIX:
                rankName = "6";
                break;
            case SEVEN:
                rankName = "7";
                break;
            case EIGHT:
                rankName = "8";
                break;
            case NINE:
                rankName = "9";
                break;
            case TEN:
                rankName = "10";
                break;
            case JACK:
                rankName = "J";
                break;
            case QUEEN:
                rankName = "Q";
                break;
            case KING:
                rankName = "K";
                break;
            default:
                rankName = "";
                break;
        }
    
        return suitName + "_" + rankName;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Card other = (Card) obj;
        return this.suit == other.suit && this.rank == other.rank;
    }

    public static boolean moveCards(List<Card> from, List<Card> to, List<Card> cards) {
        List<Card> fromCopy = new ArrayList<>(from); // 防止修改原始参数
        List<Card> toCopy = new ArrayList<>(to); // 防止修改原始参数

        System.out.println("Start moving cards: ");
        System.out.println("From: "+fromCopy);
        System.out.println("To: "+toCopy);
        System.out.println("Cards: "+cards);

        // 1. 验证输入
        if (from == null || to == null || cards == null || cards.isEmpty()) {
            return false;
        }

        // 2. 检查所有 card 是否存在于 from 中（假设 Card 已正确重写 equals()）
        if (!from.containsAll(cards)) {
            return false;
        }

        // 3. 执行移动（避免并发问题）
        try {
            // 注意必须先add在remove！因为cards和from可能是同一个
            to.addAll(cards);
            from.removeAll(cards); // 依赖 Card.equals()
            System.out.println("End moving cards: ");
            System.out.println("From: "+from);
            System.out.println("To: "+to);
            return true;
        } catch (Exception e) {
            // 处理可能的并发异常或操作失败
            from.clear();
            from.addAll(fromCopy);
            to.clear();
            to.addAll(toCopy);
            System.out.println("Error moving cards: "+cards);
            return false;
        }
    }

    public static boolean moveAllCards(List<Card> from, List<Card> to) {
        boolean success = moveCards(from, to, from);
        if (!success) {
            return false;
        }
        from.clear();
        return true;
    }


}
