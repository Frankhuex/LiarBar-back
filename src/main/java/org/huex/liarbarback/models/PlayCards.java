package org.huex.liarbarback.models;
import java.util.List;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class PlayCards {
    List<Card> cards;
    Card.Rank claimRank;

}
