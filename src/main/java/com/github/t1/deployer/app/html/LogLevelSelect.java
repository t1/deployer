package com.github.t1.deployer.app.html;

import static com.github.t1.log.LogLevel.*;

import com.github.t1.deployer.app.html.builder.HtmlBuilder;
import com.github.t1.log.LogLevel;

public class LogLevelSelect extends HtmlBuilder {
    private boolean autoSubmit = false;
    private final LogLevel level;

    public LogLevelSelect(LogLevel level) {
        this.level = level;
    }

    public LogLevelSelect autoSubmit() {
        this.autoSubmit = true;
        return this;
    }

    @Override
    public HtmlBuilder close() {
        append("<select name=\"level\"");
        if (autoSubmit)
            rawAppend(" onchange=\"this.form.submit()\"");
        rawAppend(">\n");
        in();
        for (LogLevel level : LogLevel.values())
            if (level != _DERIVED_)
                levelOption(level);
        out();
        append("</select>\n");
        return super.close();
    }

    private void levelOption(LogLevel level) {
        append("<option").append(selected(level)).append(">").append(level).append("</option>\n");
    }

    private String selected(LogLevel level) {
        return (this.level == level) ? " selected" : "";
    }
}
