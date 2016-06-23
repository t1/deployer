package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.Item;
import com.github.t1.deployer.model.Version;
import com.github.t1.deployer.repository.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;

class Variables {
    private static final Pattern VAR = Pattern.compile("\\$\\{(.*)\\}");

    private Map<String, String> variables;

    private Map<String, String> variables() {
        if (variables == null)
            //noinspection unchecked
            variables = new HashMap<>((Map<String, String>) (Map) System.getProperties());
        return variables;
    }

    public void resolve(ConfigurationPlan plan) {
        Map<GroupId, Map<ArtifactId, Item>> groupMap = plan.getGroupMap();
        for (GroupId groupId : new HashSet<>(groupMap.keySet())) {
            Map<ArtifactId, Item> artifactMap = groupMap.get(groupId);
            for (ArtifactId artifactId : new HashSet<>(artifactMap.keySet())) {
                resolve(artifactMap.get(artifactId));
                resolve(artifactId.getValue(), newId -> replaceKey(artifactMap, artifactId, new ArtifactId(newId)));
            }
            resolve(groupId.getValue(), newId -> replaceKey(groupMap, groupId, new GroupId(newId)));
        }
    }

    private void resolve(Item item) {
        if (item.getVersion() != null)
            resolve(item.getVersion().getValue(), resolved -> item.setVersion(new Version(resolved)));
    }

    public <K, V> V replaceKey(Map<K, V> map, K oldId, K newId) { return map.put(newId, map.remove(oldId)); }

    public void resolve(String key, Consumer<String> apply) {
        Matcher matcher = VAR.matcher(key);
        if (matcher.matches()) {
            String name = matcher.group(1);
            String newId = variables().get(name);
            apply.accept(newId);
        }
    }
}
