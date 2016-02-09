package factorization.colossi;

import com.google.common.base.Joiner;
import factorization.shared.Core;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class MaskLoader {
    public static ArrayList<MaskTemplate> mask_templates = new ArrayList();
    
    public static void addMask(MaskTemplate mask) {
        mask_templates.add(mask);
    }
    
    public static MaskTemplate pickMask(Random rand, EnumFacing anchor_direction, int min_size, int max_size) {
        ArrayList<MaskTemplate> valid = new ArrayList();
        int total_weight = 0;
        for (MaskTemplate mask : mask_templates) {
            if (mask.anchor != anchor_direction) continue;
            if (min_size <= mask.anchor_points && mask.anchor_points <= max_size) {
                total_weight += mask.weight;
                valid.add(mask);
            }
        }
        double aim = total_weight * rand.nextDouble();
        int weight = 0;
        for (MaskTemplate mask : valid) {
            weight += mask.weight;
            if (aim < weight) {
                return mask;
            }
        }
        return null;
    }
    
    public static void mask(String lore, int weight, String... template) {
        MaskTemplate original = new MaskTemplate(template);
        original.lore = lore;
        String[] flipped = new String[template.length];
        boolean any = false;
        for (int i = 0; i < template.length; i++) {
            String line = template[i];
            String reversed = new StringBuilder(line).reverse().toString();
            reversed = reversed.replace("<", "L").replace(">", "<").replace("L", ">");
            flipped[i] = reversed;
            any |= !reversed.equals(line);
        }
        if (any) {
            MaskTemplate reversedMask = new MaskTemplate(flipped);
            original.weight = weight / 2;
            reversedMask.weight = weight / 2;
            reversedMask.lore = lore;
            addMask(original);
            addMask(reversedMask);
        } else {
            original.weight = weight;
            addMask(original);
        }
    }
    
    public static void reloadMasks() {
        mask_templates.clear();
        loadMasks();
    }
    
    public static void loadMasks() {
        String resource_name = "/colossus_masks.txt";
        try {
            InputStream is = ColossalBuilder.class.getResourceAsStream(resource_name);
            readMasks(is);
            shareLore();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load masks", e);
        }
    }

    private static void shareLore() {
        HashSet<String> loreSet = new HashSet<String>();
        for (MaskTemplate mask : mask_templates) {
            if (StringUtils.isNullOrEmpty(mask.lore)) continue;
            loreSet.add(mask.lore);
        }
        if (loreSet.isEmpty()) return;
        ArrayList<String> lores = new ArrayList<String>(loreSet.size());
        lores.addAll(loreSet);
        Random random = new Random(88888888);
        for (MaskTemplate mask : mask_templates) {
            if (StringUtils.isNullOrEmpty((mask.lore))) {
                mask.lore = lores.get(random.nextInt(lores.size()));
            }
        }
    }
    
    private static int weight = 100;
    public static void readMasks(InputStream is) throws IOException {
        if (is == null) {
            throw new NullPointerException();
        }
        try {
            int lineNumber = 0;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            ArrayList<String> set = new ArrayList<String>();
            String lore = "";
            while (true) {
                String line = br.readLine();
                lineNumber++;
                if (line == null) {
                    emitMask(lineNumber, set, lore);
                    lore = "";
                    break;
                }
                line = line.replace("\n", "").replace("\r", "");
                if (line.startsWith("!")) {
                    lore += line.replaceFirst("!", "");
                }
                if (line.length() == 0) {
                    emitMask(lineNumber, set, lore);
                    lore = "";
                    continue;
                }
                if (line.startsWith("'")) {
                    continue;
                }
                if (line.startsWith("weight ")) {
                    weight = Integer.parseInt(line.split(" ")[1]);
                    continue;
                }
                set.add(line);
            }
        } finally {
            is.close();
        }
    }
    
    private static void emitMask(int lineNumber, ArrayList<String> template, String lore) {
        if (!template.isEmpty()) {
            String[] mask = template.toArray(new String[template.size()]);
            try {
                mask(lore, weight, mask);
            } catch (Throwable t) {
                if (Core.dev_environ) {
                    Core.logSevere("Near line " + lineNumber);
                    Core.logSevere("Parsing template: " + "\n" + Joiner.on("\n").join(template));
                }
                CrashReport crashreport = CrashReport.makeCrashReport(t, "Loading mask data");
                CrashReportCategory maskInfo = crashreport.makeCategory("Mask Info");
                maskInfo.addCrashSection("Near line", lineNumber);
                maskInfo.addCrashSection("Parsing template", "\n" + Joiner.on("\n").join(template));
                throw new ReportedException(crashreport);
            }
        }
        weight = 100;
        template.clear();
    }
    
    
}
