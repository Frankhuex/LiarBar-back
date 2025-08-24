package org.huex.liarbarback.models;

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
    public String toString() {
        return rank.toString() + " of " + suit.toString();
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
            from.removeAll(cards); // 依赖 Card.equals()
            to.addAll(cards);
            return true;
        } catch (Exception e) {
            // 处理可能的并发异常或操作失败
            return false;
        }
    }

    public static boolean moveAllCards(List<Card> from, List<Card> to) {
        to.addAll(from);
        from.clear();
        return true;
    }


}
