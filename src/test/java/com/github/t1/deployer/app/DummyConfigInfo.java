package com.github.t1.deployer.app;

import static lombok.AccessLevel.*;

import java.util.function.Function;

import javax.json.*;

import com.github.t1.config.ConfigInfo;

import lombok.*;

@Getter
@Builder
@ToString
@AllArgsConstructor(access = PRIVATE)
class DummyConfigInfo implements ConfigInfo {
    private String name, description, defaultValue;
    private Class<?> type, container;
    private JsonObject meta = Json.createObjectBuilder().build();
    private Object value;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Function<String, Object> converter = (Function) Function.identity();

    public <T> DummyConfigInfo(String name, T value, Class<T> type, Function<String, Object> converter) {
        this(name, value);
        this.type = type;
        this.converter = converter;
    }

    public DummyConfigInfo(String name, Object value) {
        this.name = name;
        this.value = value;
        this.type = value.getClass();
    }

    @Override
    public void updateTo(String value) {
        this.value = converter.apply(value);
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }
}
