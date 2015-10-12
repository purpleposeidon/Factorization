package factorization.truth.export;


import factorization.truth.AbstractTypesetter;
import factorization.truth.Tokenizer;
import factorization.truth.WordPage;
import factorization.truth.api.TruthError;
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
    protected void handleCommand(Tokenizer tokenizer, String cmd, String link, String style) throws TruthError {
        if (cmd.equalsIgnoreCase("\\topic")) {
            String subject = tokenizer.getParameter("topic item name");
            IndexDocumentation.foundTopic(subject, filename);
        } else if (cmd.equalsIgnoreCase("\\link") || cmd.equalsIgnoreCase("\\index")) {
            String target = tokenizer.getParameter("link target");
            tokenizer.getParameter("link content");
            IndexDocumentation.foundLink(target);
        }
        
    }
    
    
    @Override
    public WordPage newPage() { return null; }
    
    @Override
    public void write(Word w) { }
}
