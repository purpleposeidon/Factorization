package factorization.truth.export;

import com.google.common.collect.ArrayListMultimap;
import factorization.truth.DocumentationModule;
import factorization.truth.api.TruthError;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.*;
import net.minecraft.util.ResourceLocation;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class IndexDocumentation {
    static ArrayListMultimap<String, String> index = ArrayListMultimap.create();
    static HashSet<String> foundLinks = new HashSet<String>();
    static ArrayList<String> pendingLinks = new ArrayList<String>();

    static String domain;
    
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: IndexDocumentation outputFile (domain=path)+");
            System.exit(1);
            return;
        }
        final HashMap<String, File> domains = new HashMap<String, File>();
        boolean first = true;
        PrintStream out = null;
        for (String arg : args) {
            if (first) {
                out = new PrintStream(arg);
                first = false;
            } else {
                String[] d = arg.split("\\=");
                domains.put(d[0], new File(d[1]));
                if (domain == null) {
                    domain = d[0];
                }
            }
        }
        DocumentationModule.overrideResourceManager = new IResourceManager() {
            @Override
            public Set getResourceDomains() {
                HashSet<String> ret = new HashSet<String>();
                ret.add(domain);
                return ret;
            }
            
            @Override
            public IResource getResource(final ResourceLocation location) {
                return new IResource() {
                    @Override public boolean hasMetadata() { return false; }

                    @Override
                    public String getResourcePackName() {
                        return "DocumentationIndexer";
                    }

                    @Override public IMetadataSection getMetadata(String var1) { return null; }

                    @Override
                    public ResourceLocation getResourceLocation() {
                        return location;
                    }

                    @Override
                    public InputStream getInputStream() {
                        File domainFile = domains.get(location.getResourceDomain());
                        if (domainFile == null) return null; //this
                        String fname = domainFile.getAbsolutePath() + File.separator + location.getResourcePath();
                        try {
                            return new FileInputStream(new File(fname));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                };
            }
            
            @Override
            public List getAllResources(ResourceLocation location) {
                return Collections.singletonList(getResource(location));
            }
        };
        crawlDocuments("index", out);
    }
    
    static void resetInfo() {
        foundLinks.clear();
        pendingLinks.clear();
    }
    
    static void crawlDocuments(String rootName, PrintStream log) {
        resetInfo();
        foundLink(rootName);
        
        while (!pendingLinks.isEmpty()) {
            String link = pendingLinks.remove(0);
            String text = DocumentationModule.readDocument(domain, link);
            IndexerTypesetter ts = new IndexerTypesetter(domain, link);
            try {
                ts.write(text);
            } catch (TruthError truthError) {
                truthError.printStackTrace();
            }
        }
        
        for (Entry<String, String> entry : index.entries()) {
            log.println(entry.getKey().trim() + "\t" + entry.getValue().trim());
        }
    }
    
    static IResourceManager getMinecraftResources() {
        final IMetadataSerializer metadataSerializer_ = new IMetadataSerializer();
        metadataSerializer_.registerMetadataSectionType(new TextureMetadataSectionSerializer(), TextureMetadataSection.class);
        metadataSerializer_.registerMetadataSectionType(new FontMetadataSectionSerializer(), FontMetadataSection.class);
        metadataSerializer_.registerMetadataSectionType(new AnimationMetadataSectionSerializer(), AnimationMetadataSection.class);
        metadataSerializer_.registerMetadataSectionType(new PackMetadataSectionSerializer(), PackMetadataSection.class);
        metadataSerializer_.registerMetadataSectionType(new LanguageMetadataSectionSerializer(), LanguageMetadataSection.class);
        
        return new SimpleReloadableResourceManager(metadataSerializer_);
    }
    
    static void foundLink(String linktarget) {
        if (foundLinks.add(linktarget)) {
            pendingLinks.add(linktarget);
        }
    }
    
    static void foundTopic(String topicname, String page) {
        index.put(topicname, page);
    }
}
