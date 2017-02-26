package com.github.t1.deployer.testtools;

import com.github.t1.testtools.SystemOutCaptorRule;
import org.junit.*;

import java.net.UnknownHostException;

import static com.github.t1.deployer.testtools.CipherFacadeMain.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

public class CipherFacadeMainTest {
    private static final String PLAIN_TEXT = "foo";
    @SuppressWarnings("SpellCheckingInspection") private static final String PUBLIC_CIPHER_TEXT
            = "7780CE2D448C1631BECC020B72E6022BFA38E37D4EEB6FAB3891949C0FF591BE2C64761AE03ED294D1E0D"
            + "D9A3853FB035E92D1A0B74FFCAF37F3728D1F1CA7A1D64474F1CF485C30FF6D969CA73BF87FE3A3B076F6"
            + "C0B4F2D3443C615B7A2E4EE33AA2FC2478E61341D5DE8A614B3F23613018CAECEAB4D0BC00B3BB4AA57E7"
            + "5A6223D7B119D45892E66B048EC99A951D5C5000AB0FBFB9B95C27FEE7593749B4234826D6D071750D65A"
            + "E1245648E100EDD97A0DC2B3AFADCF7FA53871132F3FDAFD652638CB90AC721EAB397AF701393FFE82B15"
            + "27247ED0F0DE7B95D3BE0A21004670A6083437F28A2199B773FA030AE0976F888D6E4AE656FF6B6AC81BB5E";
    private static final String PRIVATE_CIPHER_TEXT = "85F84F3EB7246FBC1F73AB282979F690";

    @Rule public final SystemOutCaptorRule out = new SystemOutCaptorRule();

    @Test
    public void shouldEncryptPublic() throws Exception {
        main("--keystore", "src/test/resources/test.keystore",
                "--storetype", "jceks",
                "--alias", "keypair",
                PLAIN_TEXT);

        // the cipher text is not reproducible :-(
        assertThat(out.out().length()).isEqualTo(PUBLIC_CIPHER_TEXT.length());
    }

    @Test
    public void shouldEncryptCert() throws Exception {
        main("--keystore", "src/test/resources/cert.keystore",
                "--storetype", "jceks",
                "--alias", "cert",
                PLAIN_TEXT);

        // the cipher text is not reproducible :-(
        assertThat(out.out().length()).isEqualTo(PUBLIC_CIPHER_TEXT.length());
    }

    @Test
    public void shouldDecryptPublic() throws Exception {
        main("--keystore", "src/test/resources/test.keystore",
                "--storetype", "jceks",
                "--alias", "keypair",
                "--decrypt", PUBLIC_CIPHER_TEXT);

        assertThat(out.out()).isEqualTo(PLAIN_TEXT);
    }

    @Test
    public void shouldEncryptPrivate() throws Exception {
        main("--keystore", "src/test/resources/test.keystore",
                "--storetype", "jceks",
                "--alias", "secretkey",
                PLAIN_TEXT);

        assertThat(out.out()).isEqualTo(PRIVATE_CIPHER_TEXT);
    }

    @Test
    public void shouldDecryptPrivate() throws Exception {
        main("--keystore", "src/test/resources/test.keystore",
                "--storetype", "jceks",
                "--alias", "secretkey",
                "--decrypt", PRIVATE_CIPHER_TEXT);

        assertThat(out.out()).isEqualTo(PLAIN_TEXT);
    }

    @Test
    public void shouldEncryptPublicToUri() throws Exception {
        Throwable e = catchThrowable(() -> {
            main("--uri", "https://www.github.com", PLAIN_TEXT);

            // the cipher text is not reproducible :-(
            assertThat(out.out().length()).isEqualTo(PUBLIC_CIPHER_TEXT.length());
        });

        if (e instanceof UnknownHostException)
            assumeNoException(e);
    }
}
