package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.ChecksumX;
import com.github.t1.problem.WebApplicationApplicationException;

public class ErrorWhileFetchingChecksumException extends WebApplicationApplicationException {
    protected ErrorWhileFetchingChecksumException(ChecksumX checksum) {
        super("error while searching for checksum: '" + checksum + "'");
    }
}
