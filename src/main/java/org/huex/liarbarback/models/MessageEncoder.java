package org.huex.liarbarback.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;

// MessageEncoder.java
public class MessageEncoder implements Encoder.Text<Message<?>> {
  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public String encode(Message message) throws EncodeException {
    try {
      return mapper.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      throw new EncodeException(message, "序列化失败", e);
    }
  }
}