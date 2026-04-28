package com.onboardguard.auth.service.impl;

import com.onboardguard.auth.service.OfficerCredentialGenerator;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class OfficerCredentialGeneratorImpl implements OfficerCredentialGenerator {

    private static final String UPPER  = "ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final String LOWER  = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOL = "@#$!";
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    public String generatePassword() {
        String raw = pick(UPPER, 2) + pick(LOWER, 3) + pick(DIGITS, 2)
                + pick(SYMBOL, 1) + pick(UPPER + LOWER, 2);
        return shuffle(raw);
    }

    private String pick(String chars, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(chars.charAt(RNG.nextInt(chars.length())));
        return sb.toString();
    }

    private String shuffle(String s) {
        char[] ch = s.toCharArray();
        for (int i = ch.length - 1; i > 0; i--) {
            int j = RNG.nextInt(i + 1);
            char tmp = ch[i]; ch[i] = ch[j]; ch[j] = tmp;
        }
        return new String(ch);
    }
}