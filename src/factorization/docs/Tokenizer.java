package factorization.docs;

import static factorization.docs.Tokenizer.TokenType.*;

public class Tokenizer {
    public static enum TokenType {
        WORD, COMMAND, PARAMETER
    }
    
    public TokenType type;
    public String token;
    
    private final String src;
    private int scan = 0;
    
    public Tokenizer(String src) {
        this.src = src;
    }
    
    private static final String NL = "\\p";
    private static final String WS = "\\ ";
    
    private int contigLines = 0;
    private int contigSpaces = 0;
    
    public boolean nextToken() {
        while (true) {
            if (scan + 1 >= src.length()) return false;
            char c = src.charAt(scan);
            if (c == '\n') {
                scan++;
                contigLines++;
                if (contigLines == 1) {
                    type = COMMAND;
                    token = WS;
                    return true;
                }
                if (contigLines == 2) {
                    type = COMMAND;
                    token = NL;
                    return true;
                }
                continue;
            } else if (c == '\r') {
                continue;
            } else if (Character.isWhitespace(c)) {
                scan++;
                contigSpaces++;
                if (contigSpaces == 1) {
                    type = COMMAND;
                    token = WS;
                    return true;
                }
                continue;
            } else if (c == '\\') {
                readCommand();
            } else if (c == '{') {
                readParameter();
            } else {
                readWord();
            }
            contigLines = contigSpaces = 0;
            return true;
        }
    }

    
    private void emit(TokenType type, int start, int end) {
        this.type = type;
        token = src.substring(start, end);
    }

    private void readWord() {
        final int start = scan;
        while (true) {
            scan++;
            if (scan >= src.length() ) {
                break;
            }
            char c = src.charAt(scan);
            if (Character.isWhitespace(c) || c == '\\' || c == '{') {
                break;
            }
        }
        emit(WORD, start, scan);
    }

    private void readParameter() {
        final int start = scan;
        int count = 0;
        while (scan < src.length()) {
            char c = src.charAt(scan++);
            if (c == '{') {
                count++;
            } else if (c == '}') {
                count--;
            }
            if (count <= 0) break;
        }
        emit(PARAMETER, start + 1, scan - 1);
    }



    private void readCommand() {
        readWord();
        type = COMMAND;
        if (scan < src.length() - 1) {
            if (Character.isWhitespace(src.charAt(scan))) {
                scan++;
            }
        }
    }
}
