package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Config;
import com.github.t1.deployer.tools.CipherService;
import com.github.t1.deployer.tools.KeyStoreConfig;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/ciphers")
@Stateless
@Slf4j
@AllArgsConstructor @NoArgsConstructor
public class CipherBoundary {
    @Inject
    CipherService cipher;

    @Inject @Config("key-store")
    KeyStoreConfig keyStore;

    @POST
    @Path("/encrypt")
    public String encrypt(String body) {
        return cipher.encrypt(body, keyStore);
    }
}
