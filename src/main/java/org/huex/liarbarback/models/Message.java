package org.huex.liarbarback.models;


import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Message<T> {

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    public enum MsgType {
        // Request
        CREATE_ROOM, // null
        JOIN_ROOM, // String roomId
        LEAVE_ROOM, // null
        CHANGE_NAME, // String newName
        GET_ROOM_PLAYERS, // null
        PREPARE, // boolean isReady
        CANCEL_PREPARE, 
        START_GAME, // boolean
        PLAY_CARDS, // String cardId
        SKIP,
        CHALLENGE,
        RESTART,

        // Response
        WELCOME,
        ROOM_CREATED,
        ROOM_JOINED,
        ROOM_LEFT,
        NAME_CHANGED,
        ROOM_PLAYERS_LIST,
        PREPARED,
        CANCELLED_PREPARE,
        GAME_STARTED,

        // Error
        ERROR,
        INVALID_REQUEST,
        ROOM_NOT_FOUND,
        ALREADY_IN_ROOM,
        NAME_ALREADY_EXISTS,
        GAME_ALREADY_STARTED,
        NOT_PREPARED,
        NOT_IN_ROOM,
        ALREADY_PREPARED,
        ALREADY_STARTED,
        GAME_NOT_STARTED,
        GAME_ALREADY_FINISHED,
        GAME_NOT_FOUND,
        PLAYER_NOT_FOUND,
    }

    private String msgId;
    private MsgType msgType;
    private T data;

    public Message(MsgType msgType, T data) {
        this.msgId = java.util.UUID.randomUUID().toString();
        this.msgType = msgType;
        this.data = data;
    }
}
