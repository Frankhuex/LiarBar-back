package org.huex.liarbarback.events;


import org.springframework.context.ApplicationEvent;

import lombok.*;

@Getter
public class RoomUpdatedEvent extends ApplicationEvent {
    private String roomId;
    public RoomUpdatedEvent(Object source, String roomId) {
        super(source);
        this.roomId = roomId;
    }
}
