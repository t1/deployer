package com.github.t1.deployer.container;

import com.github.t1.log.LogLevel;
import com.google.common.collect.ImmutableMap;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.github.t1.deployer.container.CLI.*;
import static com.github.t1.deployer.container.LogHandlerName.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;

@Slf4j
@Builder(builderMethodName = "doNotCallThisBuilderExternally")
@Accessors(fluent = true, chain = true)
public class LogHandlerResource extends AbstractResource {
    private static final String DEFAULT_FORMAT = "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n";

    @NonNull @Getter private final LoggingHandlerType type;
    @NonNull @Getter private final LogHandlerName name;

    private LogLevel level;
    private String format;
    private String formatter;

    private String file;
    private String suffix;

    private String module;
    private String class_;
    private Map<String, String> properties;

    private LogHandlerResource(LoggingHandlerType type, LogHandlerName name, CLI cli) {
        super(cli);
        this.type = type;
        this.name = name;
    }

    public static LogHandlerResourceBuilder builder(LoggingHandlerType type, LogHandlerName name, CLI cli) {
        return doNotCallThisBuilderExternally().cli(cli).type(type).name(name);
    }

    public static List<LogHandlerResource> allHandlers(CLI cli) {
        return Arrays.stream(LoggingHandlerType.values())
                     .flatMap(type -> {
                         ModelNode request = readResource(
                                 new LogHandlerResource(type, ALL, cli).createRequestWithAddress());
                         return cli.execute(request)
                                   .asList().stream()
                                   .map(node -> toLoggerResource(type(node), name(node), cli, node.get("result")));
                     })
                     .sorted(comparing(LogHandlerResource::name))
                     .collect(toList());
    }

    private static LoggingHandlerType type(ModelNode node) {
        return LoggingHandlerType.valueOfHandlerName(new ArrayList<>(node.get("address").get(1).keys()).get(0));
    }

    private static LogHandlerName name(ModelNode node) {
        return new LogHandlerName(node.get("address").get(1).get(type(node).getHandlerName()).asString());
    }

    private static LogHandlerResource toLoggerResource(LoggingHandlerType type, LogHandlerName name, CLI cli,
            ModelNode node) {
        LogHandlerResource logger = new LogHandlerResource(type, name, cli);
        logger.readFrom(node);
        logger.deployed = true;
        return logger;
    }

    public static class LogHandlerResourceBuilder {
        private CLI cli;

        public LogHandlerResourceBuilder cli(CLI cli) {
            this.cli = cli;
            return this;
        }

        public LogHandlerResource build() {
            LogHandlerResource resource = new LogHandlerResource(type, name, cli);
            resource.level = level;
            resource.format = format;
            resource.formatter = formatter;

            resource.file = file;
            resource.suffix = suffix;

            resource.module = module;
            resource.class_ = class_;
            resource.properties = properties;

            return resource;
        }
    }

    public LogLevel level() {
        checkDeployed();
        return level;
    }

    public String format() {
        checkDeployed();
        return format;
    }

    public String formatter() {
        checkDeployed();
        return formatter;
    }

    public String file() {
        checkDeployed();
        return file;
    }

    public String suffix() {
        checkDeployed();
        return suffix;
    }

    public String module() {
        checkDeployed();
        return module;
    }

    public String class_() {
        checkDeployed();
        return class_;
    }

    public Map<String, String> properties() {
        checkDeployed();
        return properties;
    }

    @Override public String toString() {
        return type + ":" + name + ((deployed == null) ? ":?" : deployed ? ":deployed" : ":undeployed") + ":" + level
                + ((format == null) ? "" : ":" + format)
                + ((formatter == null) ? "" : ":" + formatter)
                + ((file == null) ? "" : ":" + file)
                + ((suffix == null) ? "" : ":" + suffix)
                + ((module == null) ? "" : ":" + module)
                + ((class_ == null) ? "" : ":" + class_)
                + ((properties == null) ? "" : ":" + properties);
    }

    public void updateLevel(LogLevel newLevel) {
        checkDeployed();
        writeAttribute("level", newLevel.name());
    }

    public void updateFormat(String newFormat) {
        checkDeployed();
        writeAttribute("formatter", newFormat);
    }

    public void updateFormatter(String newFormatter) {
        checkDeployed();
        writeAttribute("named-formatter", newFormatter);
    }

    public void updateFile(String newFile) {
        checkDeployed();
        writeAttribute("file", newFile);
    }

    public void updateSuffix(String newSuffix) {
        checkDeployed();
        writeAttribute("suffix", newSuffix);
    }

    public void updateModule(String newModule) {
        checkDeployed();
        writeAttribute("module", newModule);
    }

    public void updateClass(String newClass) {
        checkDeployed();
        writeAttribute("class", newClass);
    }

    public void addProperty(String key, String value) {
        checkDeployed();
        mapPut("property", key, value);
    }

    public void updateProperty(String key, String value) {
        checkDeployed();
        mapPut("property", key, value);
    }

    public void removeProperty(String key) {
        checkDeployed();
        mapRemove("property", key);
    }


    @Override protected ModelNode createRequestWithAddress() {
        ModelNode request = new ModelNode();
        request.get("address")
               .add("subsystem", "logging")
               .add(type.getHandlerName(), name.getValue());
        return request;
    }

    @Override protected void readFrom(ModelNode result) {
        this.level = (result.get("level").isDefined()) ? LogLevel.valueOf(result.get("level").asString()) : null;
        this.format = readFormat(result);
        this.formatter = getString(result, "named-formatter");
        this.file = (result.get("file").isDefined()) ? result.get("file").get("path").asString() : null;
        this.suffix = getString(result, "suffix");
        this.module = getString(result, "module");
        this.class_ = getString(result, "class");
        this.properties = getMap(result.get("properties"));
    }

    @Nullable private String readFormat(ModelNode result) {
        ModelNode node = result.get("formatter");
        return (node.isDefined() && !DEFAULT_FORMAT.equals(node.asString())) ? node.asString() : null;
    }

    private static String getString(ModelNode node, String name) {
        ModelNode value = node.get(name);
        return (value.isDefined()) ? value.asString() : null;
    }

    private static Map<String, String> getMap(ModelNode value) {
        if (!value.isDefined())
            return null;
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        value.asPropertyList().forEach(property -> map.put(property.getName(), property.getValue().asString()));
        return map.build();
    }

    @Override public void add() {
        log.debug("add log handler {}", name);
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("add");
        if (level != null)
            request.get("level").set(level.name());
        if (format != null)
            request.get("formatter").set(format);
        if (formatter != null)
            request.get("named-formatter").set(formatter);

        if (file != null) {
            request.get("file").get("path").set(file);
            request.get("file").get("relative-to").set("jboss.server.log.dir");
        }
        if (suffix != null)
            request.get("suffix").set(suffix);

        if (module != null)
            request.get("module").set(module);
        if (class_ != null)
            request.get("class").set(class_);
        if (properties != null)
            properties.forEach((key, value) -> request.get("properties").add(key, value));

        execute(request);

        this.deployed = true;
    }
}
