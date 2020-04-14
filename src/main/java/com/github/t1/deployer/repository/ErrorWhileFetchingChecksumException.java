package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Checksum;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.problemdetails.Extension;
import org.eclipse.microprofile.problemdetails.Status;

import static org.eclipse.microprofile.problemdetails.ResponseStatus.BAD_REQUEST;

@Status(BAD_REQUEST) @AllArgsConstructor
public class ErrorWhileFetchingChecksumException extends RuntimeException {
    @Extension Checksum checksum;
}
