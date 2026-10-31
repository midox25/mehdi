package com.mehdi.barcahome;

import java.text.Collator;
import java.util.*;

/** Platform-independent catalog rules; Android supplies real activity candidates. */
public final class AppCatalog {
    private AppCatalog() {}
    public static final class Candidate {
        public final String key, pkg, label;
        public final boolean enabled, exported, suspended, tv;
        public Candidate(String key,String pkg,String label,boolean enabled,boolean exported,boolean suspended,boolean tv) {
            this.key=key;this.pkg=pkg;this.label=label;this.enabled=enabled;this.exported=exported;this.suspended=suspended;this.tv=tv;
        }
    }
    private static final Set<String> INTERNAL_TOOLS = new HashSet<>(Arrays.asList(
        "com.duokan.factorytest","mediatek.factorymenu.ui","com.mediatek.wwtv.setupwizard",
        "com.google.android.tungsten.setupwraith","com.google.android.onetimeinitializer",
        "com.android.cts.ctsshim","com.android.cts.priv.ctsshim"));
    private static int rank(String pkg) {
        return pkg.equals("com.google.android.youtube.tv")?0:pkg.equals("com.mitv.livetv")?1:2;
    }
    public static List<Candidate> select(List<Candidate> candidates,Set<String> homePackages,String self) {
        Map<String,Candidate> apps=new HashMap<>();
        for(Candidate c:candidates) {
            if(!c.enabled || !c.exported || c.suspended || c.pkg.equals(self) ||
                homePackages.contains(c.pkg) || INTERNAL_TOOLS.contains(c.pkg))continue;
            Candidate existing=apps.get(c.pkg);
            if(existing==null || (c.tv && !existing.tv))apps.put(c.pkg,c);
        }
        List<Candidate> result=new ArrayList<>(apps.values());
        Collator collator=Collator.getInstance(Locale.FRANCE);
        result.sort((a,b)->{
            int priority=Integer.compare(rank(a.pkg),rank(b.pkg));
            if(priority!=0)return priority;
            int label=collator.compare(a.label,b.label);return label!=0?label:a.pkg.compareTo(b.pkg);
        });
        return result;
    }
}
