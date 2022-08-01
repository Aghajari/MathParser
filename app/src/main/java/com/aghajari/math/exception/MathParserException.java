package com.aghajari.math.exception;

import com.aghajari.math.Utils;

public class MathParserException extends Exception {

    final String source, localMessage;
    final int index;

    public MathParserException(String src, int index, String message) {
        super(message + generateMessages(src, index));
        this.localMessage = message;
        this.source = src;
        this.index = index;
    }

    public MathParserException(String source, String message, Throwable cause) {
        super(message, cause);
        this.localMessage = message;
        this.source = source;
        this.index = -1;
    }

    public int getIndex() {
        return index;
    }

    public String getSource() {
        return source;
    }

    public String getLocalMessage() {
        return localMessage;
    }

    public String getCursor() {
        return getCursor(index);
    }

    private static String getCursor(int index) {
        return Utils.repeat(' ', index - 1) + "^";
    }

    private static String generateMessages(String src, int index) {
        if (index == -1 || (src == null || src.isEmpty()))
            return "";

        return "\n\t" + src + "\n\t" + getCursor(index);
    }
}
