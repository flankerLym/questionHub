package com.questionhub.desktop.util;

public final class Validation {
    private Validation() {}

    public static String folderName(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("文件夹名称不能为空");
        if (v.length() > 60) throw new IllegalArgumentException("文件夹名称不能超过 60 个字符");
        return v;
    }

    public static String question(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("问题不能为空");
        if (v.length() > 500) throw new IllegalArgumentException("问题不能超过 500 个字符");
        return v;
    }

    public static String answer(String value) {
        String v = value == null ? "" : value;
        if (v.length() > 50_000) throw new IllegalArgumentException("答案不能超过 50000 个字符");
        return v;
    }
}
