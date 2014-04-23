package factorization.docs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.AnimationMetadataSectionSerializer;
import net.minecraft.client.resources.data.FontMetadataSection;
import net.minecraft.client.resources.data.FontMetadataSectionSerializer;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.resources.data.LanguageMetadataSection;
import net.minecraft.client.resources.data.LanguageMetadataSectionSerializer;
import net.minecraft.client.resources.data.PackMetadataSection;
import net.minecraft.client.resources.data.PackMetadataSectionSerializer;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.client.resources.data.TextureMetadataSectionSerializer;
import net.minecraft.util.ResourceLocation;

import com.google.common.collect.ArrayListMultimap;

public class IndexDocumentation {
    static ArrayListMultimap<String, String> index = ArrayListMultimap.create();
    static HashSet<String> foundLinks = new HashSet();
    static ArrayList<String> pendingLinks = new ArrayList();
    public static boolean isRunning = false;
    
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: IndexDocumentation outputFile (domain=path)+");
            System.exit(1);
            return;
        }
        isRunning = true;
        final HashMap<String, File> domains = new HashMap();
        boolean first = true;
        PrintStream out = null;
        for (String arg : args) {
            if (first) {
                out = new PrintStream(arg);
                first = false;
            } else {
                String[] d = arg.split("\\=");
                domains.put(d[0], new File(d[1]));
            }
        }
        DocumentationModule.overrideResourceManager = new IResourceManager() {
            @Override
            public Set getResourceDomains() {
                HashSet<String> ret = new HashSet();
                ret.add("factorization");
                return ret;
            }
            
            @Override
            public IResource getResource(final ResourceLocation location) throws IOException {
                return new IResource() {
                    @Override public boolean hasMetadata() { return false; }
                    @Override public IMetadataSection getMetadata(String var1) { return null; }
                    
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
            public List getAllResources(ResourceLocation location) throws IOException {
                return Arrays.asList(getResource(location));
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
            String text = DocumentationModule.readDocument(link);
            IndexerTypesetter ts = new IndexerTypesetter(link);
            ts.processText(text);
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
        
        IReloadableResourceManager mcResourceManager = new SimpleReloadableResourceManager(metadataSerializer_);
        return mcResourceManager;
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
