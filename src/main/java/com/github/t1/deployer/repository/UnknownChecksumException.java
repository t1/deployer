package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Checksum;
import com.github.t1.problem.WebApplicationApplicationException;

class UnknownChecksumException extends WebApplicationApplicationException {
    protected UnknownChecksumException(Checksum checksum) { super("unknown checksum: '" + checksum + "'"); }
}
