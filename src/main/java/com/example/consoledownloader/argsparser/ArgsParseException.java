package com.example.consoledownloader.argsparser;

import java.io.IOException;

public class ArgsParseException extends IOException {
    public ArgsParseException(String message) {
        super(message);
    }

    public ArgsParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
