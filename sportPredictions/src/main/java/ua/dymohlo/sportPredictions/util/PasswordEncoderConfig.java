package ua.dymohlo.sportPredictions.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordEncoderConfig {

    private PasswordEncoderConfig() {
        throw new UnsupportedOperationException("Utility class");
    }


    public static String encoderPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean checkPassword(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }
}
