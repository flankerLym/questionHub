package com.questionhub.desktop.util;

import java.util.UUID;

public final class Ids {
    private Ids() {}
    public static String create(String prefix) {
        return prefix + "_" + UUID.randomUUID();
    }
}
