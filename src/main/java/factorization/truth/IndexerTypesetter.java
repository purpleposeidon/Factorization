package factorization.truth;


import factorization.truth.word.Word;

public class IndexerTypesetter extends AbstractTypesetter {
    String filename;
    
    public IndexerTypesetter(String domain, String filename) {
        super(domain, null, 0, 0);
        this.filename = filename;
    }
    
    void printError(String msg) {
        System.err.println(filename + ": " + msg);
    }

    @Override
    protected void handleCommand(Tokenizer tokenizer, String cmd, String link, String style) {
        if (cmd.equalsIgnoreCase("\\topic")) {
            String subject = getParameter(cmd, tokenizer);
            if (subject == null) {
                printError("Missing item name parameter");
                return;
            }
            IndexDocumentation.foundTopic(subject, filename);
        } else if (cmd.equalsIgnoreCase("\\link") || cmd.equalsIgnoreCase("\\index")) {
            String target = getParameter(cmd, tokenizer);
            if (target == null) {
                printError("Missing a link target");
                return;
            }
            IndexDocumentation.foundLink(target);
        }
        
    }
    
    
    @Override
    WordPage newPage() { return null; }
    
    @Override
    public void emitWord(Word w) { }
}
