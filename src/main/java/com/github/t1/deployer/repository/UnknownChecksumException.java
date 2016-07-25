package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.ChecksumX;
import com.github.t1.problem.WebApplicationApplicationException;

public class UnknownChecksumException extends WebApplicationApplicationException {
    protected UnknownChecksumException(ChecksumX checksum) {
        super("unknown checksum: '" + checksum + "'");
    }
}
