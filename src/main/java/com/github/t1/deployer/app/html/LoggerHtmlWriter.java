package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.model.LoggerConfig.*;
import static com.github.t1.log.LogLevel.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.Loggers;
import com.github.t1.deployer.model.LoggerConfig;
import com.github.t1.log.LogLevel;

@Provider
public class LoggerHtmlWriter extends AbstractHtmlWriter<LoggerConfig> {
    public LoggerHtmlWriter() {
        super(LoggerConfig.class, LOGGERS);
    }

    private boolean isNew() {
        return NEW_LOGGER.equals(target.getCategory());
    }

    @Override
    protected String bodyTitle() {
        return isNew() ? "Add Logger" : target.getCategory();
    }

    @Override
    protected String title() {
        return isNew() ? "Add Logger" : "Logger: " + target.getCategory();
    }

    @Override
    protected void body() {
        in().in();
        href("&lt", Loggers.base(uriInfo)).nl();
        if (isNew()) {
            append("<p>Enter the name of a new logger to configure</p>\n");
            append("<form method=\"POST\" action=\"").append(Loggers.base(uriInfo)).append("\">\n");
            in();
            append("<input name=\"category\"/>\n");
            levelSelect();
            append("<input type=\"submit\" value=\"Add\">\n");
            out();
            append("</form>\n");
        } else {
            append("<form method=\"POST\" action=\"").append(Loggers.base(uriInfo)).append("\">\n");
            in();
            levelSelect();
            append("<input type=\"submit\" value=\"Update\">\n");
            out();
            append("</form>\n");
            delete();
        }
        out().out();
    }

    private void levelSelect() {
        append("<select name=\"level\"\n"); // onchange=\"alert(this.form.level.options[this.form.level.selectedIndex].value)\">\n"
        for (LogLevel level : LogLevel.values())
            if (level != _DERIVED_)
                levelOption(level);
        append("</select>\n");
    }

    private void levelOption(LogLevel level) {
        append("  <option").append(selected(level)).append(">").append(level).append("</option>\n");
    }

    private String selected(LogLevel level) {
        return (level == target.getLevel()) ? " selected" : "";
    }

    private void delete() {
        append("<form method=\"POST\" action=\"" + Loggers.path(uriInfo, target) + "\">\n");
        append("  <input type=\"hidden\" name=\"action\" value=\"delete\">\n");
        append("  <input type=\"submit\" value=\"Delete\">\n");
        append("</form>\n");
    }
}
