package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audits.Warning;
import com.github.t1.deployer.model.ProcessState;
import lombok.Value;

import java.util.List;

/** see {@link Audits} */
@Value
public class AuditsResponse {
    List<Audit> audits;
    List<Warning> warnings;
    ProcessState processState;
}
