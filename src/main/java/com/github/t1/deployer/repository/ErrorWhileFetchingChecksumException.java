package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Checksum;
import com.github.t1.problem.WebApplicationApplicationException;

class ErrorWhileFetchingChecksumException extends WebApplicationApplicationException {
    protected ErrorWhileFetchingChecksumException(Checksum checksum) {
        super("error while searching for checksum: '" + checksum + "'");
    }
}
