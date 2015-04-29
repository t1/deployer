package com.github.t1.deployer.app.html;

import static com.github.t1.log.LogLevel.*;

import com.github.t1.log.LogLevel;

public class LogLevelSelectForm extends AbstractHtmlWriter<LogLevel> {
    private boolean autoSubmit = false;

    public LogLevelSelectForm(LogLevel level, StringBuilder out) {
        super.target = level;
        super.out = out;
    }

    public LogLevelSelectForm autoSubmit() {
        this.autoSubmit = true;
        return this;
    }

    public void write() {
        append("<select name=\"level\"");
        if (autoSubmit)
            out.append(" onchange=\"this.form.submit()\"");
        out.append(">\n");
        for (LogLevel level : LogLevel.values())
            if (level != _DERIVED_)
                levelOption(level);
        append("</select>\n");
    }

    private void levelOption(LogLevel level) {
        append("  <option").append(selected(level)).append(">").append(level).append("</option>\n");
    }

    private String selected(LogLevel level) {
        return (level == target) ? " selected" : "";
    }

    @Override
    public LogLevelSelectForm indent(int indent) {
        super.indent(indent);
        return this;
    }
}
