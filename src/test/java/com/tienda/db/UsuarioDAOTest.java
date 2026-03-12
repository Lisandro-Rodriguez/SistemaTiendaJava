package com.tienda.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

public class UsuarioDAOTest {

    @Test
    public void testHashContrasena() {
        String password = "miContrasena123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        assertNotNull(hash);
        assertNotEquals(password, hash);
    }

    @Test
    public void testVerificarContrasenaCorrecta() {
        String password = "miContrasena123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        assertTrue(BCrypt.checkpw(password, hash));
    }

    @Test
    public void testVerificarContrasenaIncorrecta() {
        String password = "miContrasena123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        assertFalse(BCrypt.checkpw("otraContrasena", hash));
    }
}