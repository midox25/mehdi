import com.mehdi.barcahome.AppCatalog;
import java.util.*;

/** Run on a JVM: fixtures model enabled/disabled packages and duplicate TV activities. */
public final class AppCatalogTest {
    private static AppCatalog.Candidate app(String key,String pkg,String label,boolean enabled,boolean exported,boolean suspended,boolean tv) {
        return new AppCatalog.Candidate(key,pkg,label,enabled,exported,suspended,tv);
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        String self="com.mehdi.barcahome";
        Set<String> homes=new HashSet<>(Arrays.asList("com.google.android.apps.tv.launcherx","com.mitv.tvhome.atv"));
        List<AppCatalog.Candidate> fixtures=Arrays.asList(
            app("yt-phone","com.google.android.youtube.tv","YouTube",true,true,false,false),
            app("yt-tv","com.google.android.youtube.tv","YouTube",true,true,false,true),
            app("live","com.mitv.livetv","Live TV",true,true,false,true),
            app("netflix","com.netflix.ninja","Netflix",false,true,false,true),
            app("prime","com.amazon.amazonvideo.livingroom","Prime Video",false,true,false,true),
            app("forja","ma.snrt.forja.android.app","Forja",true,true,false,true),
            app("browser","com.tcl.browser","Browser",true,true,false,false),
            app("home","com.google.android.apps.tv.launcherx","Google TV",true,true,false,true),
            app("me",self,"Barca Home",true,true,false,true),
            app("test","com.duokan.factorytest","Factory",true,true,false,true),
            app("private","private.app","Private",true,false,false,true),
            app("suspended","suspended.app","Suspended",true,true,true,true));
        List<AppCatalog.Candidate> selected=AppCatalog.select(fixtures,homes,self);
        check(selected.size()==4,"Disabled, suspended, private, home and factory apps must stay hidden");
        check(selected.get(0).key.equals("yt-tv"),"Prefer TV activity over duplicate phone activity");
        check(selected.get(1).pkg.equals("com.mitv.livetv"),"Keep actual Live TV second");
        check(selected.get(2).label.equals("Browser") && selected.get(3).label.equals("Forja"),"Other installed apps sorted by label");
        List<AppCatalog.Candidate> reversed=new ArrayList<>(fixtures);Collections.reverse(reversed);
        check(AppCatalog.select(reversed,homes,self).get(0).key.equals("yt-tv"),"TV preference must not depend on query order");
        check(AppCatalog.select(Collections.emptyList(),homes,self).isEmpty(),"Empty catalog must not invent YouTube or Live TV");
        check(AppCatalog.select(Arrays.asList(fixtures.get(3),fixtures.get(4)),homes,self).isEmpty(),"Disabled-only catalog must remain empty");
        check(AppCatalog.select(Collections.singletonList(app("new","new.app","New app",true,true,false,true)),homes,self).size()==1,
            "A newly installed enabled app must appear without a hardcoded allowlist");
        System.out.println("PASS: 8 catalog checks (disabled apps, deduplication, TV preference, ordering, empty state, discovery)");
    }
}
