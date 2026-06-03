package org.eternity.event.step03;

public interface JsonConverter {
    <T> String toJson(T object);
}
