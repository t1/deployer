package com.github.t1.deployer.app;

import javax.json.*;

import com.github.t1.config.ConfigInfo;

import lombok.*;

@Getter
@AllArgsConstructor
class DummyConfigInfo implements ConfigInfo {
    String name;
    String value;

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getDefaultValue() {
        return null;
    }

    @Override
    public Class<?> getType() {
        return null;
    }

    @Override
    public Class<?> getContainer() {
        return null;
    }

    @Override
    public JsonObject getMeta() {
        return Json.createObjectBuilder().build();
    }

    @Override
    public void updateTo(String value) {
        this.value = value;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }
}
