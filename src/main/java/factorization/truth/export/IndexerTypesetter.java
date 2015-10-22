package factorization.truth.export;


import factorization.truth.ClientTypesetter;
import factorization.truth.api.IWord;

public class IndexerTypesetter extends ClientTypesetter {
    String filename;
    
    public IndexerTypesetter(String domain, String filename) {
        super(domain, null, 0, 0);
        this.filename = filename;
    }

    @Override
    public void writeErrorMessage(String msg) {
        System.err.println(filename + ": " + msg);
    }

    @Override
    public void write(IWord w) {
        if (active_link != null) {
            IndexDocumentation.foundLink(active_link);
        }
    }

    public void foundTopic(String topic) {
        IndexDocumentation.foundTopic(topic, filename);
    }
}
