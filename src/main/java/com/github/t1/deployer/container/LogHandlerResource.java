package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LogHandlerName;
import com.github.t1.deployer.model.*;
import com.github.t1.log.LogLevel;
import com.google.common.collect.ImmutableMap;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.Supplier;

import static com.github.t1.deployer.container.LoggerResource.*;
import static com.github.t1.deployer.model.LogHandlerName.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import static org.jboss.as.controller.client.helpers.Operations.*;

@Slf4j
@Builder(builderMethodName = "do_not_call", buildMethodName = "get")
@Accessors(fluent = true, chain = true)
@SuppressWarnings("unused")
public class LogHandlerResource extends AbstractResource<LogHandlerResource> {
    /** the format is asymmetric, i.e. when you don't write it, you'll get this when reading. */
    public static final String DEFAULT_LOG_FORMAT = "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n";

    @NonNull @Getter private final LogHandlerType type;
    @NonNull @Getter private final LogHandlerName name;

    private LogLevel level;
    private String format;
    private String formatter;
    private String encoding;

    private String file;
    private String suffix;

    private String module;
    private String class_;
    private Map<String, String> properties;

    private LogHandlerResource(LogHandlerType type, LogHandlerName name, CLI cli) {
        super(cli);
        this.type = type;
        this.name = name;
    }

    public static LogHandlerResourceBuilder builder(LogHandlerType type, LogHandlerName name, CLI cli) {
        LogHandlerResourceBuilder builder = new LogHandlerResourceBuilder().type(type).name(name);
        builder.cli = cli;
        return builder;
    }

    public static List<LogHandlerResource> allHandlers(CLI cli) {
        return Arrays.stream(LogHandlerType.values())
                     .flatMap(type -> cli.execute(createReadResourceOperation(address(type, ALL), true))
                                         .asList().stream()
                                         .map(node -> toLoggerResource(type(node), name(node), cli,
                                                 node.get("result"))))
                     .sorted(comparing(LogHandlerResource::name))
                     .collect(toList());
    }

    private static LogHandlerType type(ModelNode node) {
        return LogHandlerType.valueOfHandlerName(new ArrayList<>(node.get("address").get(1).keys()).get(0));
    }

    private static LogHandlerName name(ModelNode node) {
        return new LogHandlerName(node.get("address").get(1).get(type(node).getHandlerTypeName()).asString());
    }

    private static LogHandlerResource toLoggerResource(LogHandlerType type, LogHandlerName name, CLI cli,
            ModelNode node) {
        LogHandlerResource logger = new LogHandlerResource(type, name, cli);
        logger.readFrom(node);
        logger.deployed = true;
        return logger;
    }

    public static class LogHandlerResourceBuilder implements Supplier<LogHandlerResource> {
        private CLI cli;

        @Override public LogHandlerResource get() {
            LogHandlerResource resource = new LogHandlerResource(type, name, cli);
            resource.level = level;
            resource.format = format;
            resource.formatter = formatter;
            resource.encoding = encoding;

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

    public String encoding() {
        checkDeployed();
        return encoding;
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
                + ((encoding == null) ? "" : ":" + encoding)
                + ((file == null) ? "" : ":" + file)
                + ((suffix == null) ? "" : ":" + suffix)
                + ((module == null) ? "" : ":" + module)
                + ((class_ == null) ? "" : ":" + class_)
                + ((properties == null) ? "" : ":" + properties);
    }

    public void updateLevel(LogLevel newLevel) {
        checkDeployed();
        writeAttribute("level", newLevel.name());
        this.level = newLevel;
    }

    public void updateFormat(String newFormat) {
        checkDeployed();
        writeAttribute("formatter", newFormat);
        this.format = newFormat;
    }

    public void updateFormatter(String newFormatter) {
        checkDeployed();
        writeAttribute("named-formatter", newFormatter);
        this.formatter = newFormatter;
    }

    public void updateEncoding(String newEncoding) {
        checkDeployed();
        writeAttribute("encoding", newEncoding);
        this.encoding = newEncoding;
    }

    public void updateFile(String newFile) {
        checkDeployed();
        writeAttribute("file.path", newFile);
        this.file = newFile;
    }

    public void updateSuffix(String newSuffix) {
        checkDeployed();
        writeAttribute("suffix", newSuffix);
        this.suffix = newSuffix;
    }

    public void updateModule(String newModule) {
        checkDeployed();
        writeAttribute("module", newModule);
        this.module = newModule;
    }

    public void updateClass(String newClass) {
        checkDeployed();
        writeAttribute("class", newClass);
        this.class_ = newClass;
    }

    public void addProperty(String key, String value) {
        checkDeployed();
        propertyPut(key, value);
        this.properties = propertiesBuilder().put(key, value).build();
    }

    public void updateProperty(String key, String value) {
        checkDeployed();
        propertyPut(key, value);
        this.properties = propertiesBuilderWithout(key).put(key, value).build();
    }

    public void removeProperty(String key) {
        checkDeployed();
        propertyRemove(key);
        this.properties = propertiesBuilderWithout(key).build();
    }

    @NotNull private ImmutableMap.Builder<String, String> propertiesBuilderWithout(String key) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        this.properties.entrySet().stream().filter(e -> !e.getKey().equals(key)).forEach(builder::put);
        return builder;
    }

    private ImmutableMap.Builder<String, String> propertiesBuilder() {
        return ImmutableMap.<String, String>builder().putAll(this.properties);
    }


    @Override protected ModelNode address() { return address(type, name); }

    private static ModelNode address(LogHandlerType type, LogHandlerName name) {
        return Operations.createAddress("subsystem", "logging", type.getHandlerTypeName(), name.getValue());
    }

    @Override protected void readFrom(ModelNode result) {
        this.level = getOptional(result, "level").map(node -> mapLogLevel(node.asString())).orElse(null);
        this.format = readFormat(result);
        this.formatter = stringOrNull(result, "named-formatter");
        this.encoding = stringOrNull(result, "encoding");
        this.file = (result.get("file").isDefined()) ? result.get("file").get("path").asString() : null;
        this.suffix = stringOrNull(result, "suffix");
        this.module = stringOrNull(result, "module");
        this.class_ = stringOrNull(result, "class");
        this.properties = getMap(result.get("properties"));
    }

    @Nullable private String readFormat(ModelNode result) {
        ModelNode node = result.get("formatter");
        return (node.isDefined() && !DEFAULT_LOG_FORMAT.equals(node.asString())) ? node.asString() : null;
    }

    private static Map<String, String> getMap(ModelNode value) {
        if (!value.isDefined())
            return null;
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        value.asPropertyList().forEach(property -> map.put(property.getName(), property.getValue().asString()));
        return map.build();
    }

    @Override public void add() {
        log.debug("add log-handler {}", name);
        ModelNode request = createAddOperation(address());
        if (level != null)
            request.get("level").set(level.name());
        if (format != null)
            request.get("formatter").set(format);
        if (formatter != null)
            request.get("named-formatter").set(formatter);
        if (encoding != null)
            request.get("encoding").set(encoding);

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

    @Override public String getId() { return name.getValue(); }
}
