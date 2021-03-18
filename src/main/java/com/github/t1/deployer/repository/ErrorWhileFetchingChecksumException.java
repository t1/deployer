package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Checksum;
import com.github.t1.problemdetail.Extension;
import com.github.t1.problemdetail.Status;
import lombok.AllArgsConstructor;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Status(BAD_REQUEST) @AllArgsConstructor
public class ErrorWhileFetchingChecksumException extends RuntimeException {
    @Extension Checksum checksum;
}
