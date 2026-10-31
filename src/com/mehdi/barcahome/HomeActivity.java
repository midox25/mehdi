package com.mehdi.barcahome;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.*;

/** A local TV home: only installed, enabled, exported launchable activities are shown. */
public class HomeActivity extends Activity {
    private static final int PHOTO = 7;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService images = Executors.newSingleThreadExecutor();
    private TextView clock, heroTitle, heroDescription, heroSource, appCount;
    private ImageView heroIcon;
    private Button heroOpen, homeNav, liveNav, appsNav;
    private LinearLayout quickRow, appRow;
    private ScrollView contentScroll;
    private View firstApp, firstQuick;
    private Backdrop backdrop;
    private File loadedFile;
    private long loadedTime = -1;
    private int imageGeneration, catalogGeneration;
    private String catalogSignature = "", selectedPackage = "";
    private boolean initialFocus = true;
    private List<AppEntry> catalog = new ArrayList<>();
    private final Runnable tick = new Runnable() {
        public void run() {
            clock.setText(new SimpleDateFormat("HH:mm", Locale.FRANCE).format(new Date()));
            handler.postDelayed(this, 15000);
        }
    };
    private static class AppEntry {
        final String pkg, label;
        final Intent intent;
        final Drawable icon;
        AppEntry(String pkg, String label, Intent intent, Drawable icon) {
            this.pkg=pkg; this.label=label; this.intent=intent; this.icon=icon;
        }
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(1024,1024);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        FrameLayout frame=new FrameLayout(this);
        backdrop=new Backdrop(); frame.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(40),dp(20),dp(40),dp(16));
        frame.addView(page,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView profile=text("M",19,0xfff6eaff);profile.setGravity(Gravity.CENTER);
        profile.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        profile.setBackground(rounded(0xff605087,100,0xff9b85bd,1));
        profile.setContentDescription("Votre Mi TV");
        LinearLayout.LayoutParams avatarSpace=new LinearLayout.LayoutParams(dp(38),dp(38));
        avatarSpace.setMargins(0,0,dp(14),0);header.addView(profile,avatarSpace);
        LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabs.setPadding(dp(5),dp(4),dp(5),dp(4));tabs.setBackground(rounded(0xcc3e2a72,40,0,0));
        Button search=nav("",this::searchApps);search.setCompoundDrawablesWithIntrinsicBounds(new NavMark(false),null,null,null);search.setContentDescription("Rechercher une application installée");
        tabs.addView(search,new LinearLayout.LayoutParams(dp(48),dp(40)));
        homeNav=nav("Accueil",() -> {
            selectTab(0);contentScroll.smoothScrollTo(0,0);heroOpen.requestFocus();
        });homeNav.setSelected(true);
        tabs.addView(homeNav,new LinearLayout.LayoutParams(dp(100),dp(40)));
        liveNav=nav("Direct",() -> { AppEntry live=findApp("com.mitv.livetv"); if(live!=null)openApp(live); });
        liveNav.setVisibility(View.GONE);
        tabs.addView(liveNav,new LinearLayout.LayoutParams(dp(82),dp(40)));
        appsNav=nav("Apps",() -> {selectTab(1);if(firstApp!=null)firstApp.requestFocus();});
        tabs.addView(appsNav,new LinearLayout.LayoutParams(dp(78),dp(40)));
        header.addView(tabs);
        Button settings=nav("",this::options);settings.setCompoundDrawablesWithIntrinsicBounds(new NavMark(true),null,null,null);settings.setContentDescription("Réglages et personnalisation");
        LinearLayout.LayoutParams gearSpace=new LinearLayout.LayoutParams(dp(46),dp(42));
        gearSpace.setMargins(dp(10),0,0,0);header.addView(settings,gearSpace);
        Space spacer=new Space(this);header.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        clock=text("",20,Color.WHITE);header.addView(clock);
        TextView name=text("  |  Mi TV",13,0xffd8c8ed);header.addView(name);
        page.addView(header,new LinearLayout.LayoutParams(-1,dp(52)));

        contentScroll=new ScrollView(this);contentScroll.setFillViewport(true);
        contentScroll.setVerticalScrollBarEnabled(false);contentScroll.setSmoothScrollingEnabled(true);
        contentScroll.setClipChildren(false);page.setClipChildren(false);
        page.addView(contentScroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);contentScroll.addView(content);
        int screenH=(int)(getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density);
        int heroHeight=Math.max(160,Math.min(240,(int)(screenH*.39f)));
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);hero.setPadding(0,dp(12),0,dp(6));
        LinearLayout source=new LinearLayout(this);source.setGravity(Gravity.CENTER_VERTICAL);
        heroIcon=new ImageView(this);heroIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        source.addView(heroIcon,new LinearLayout.LayoutParams(dp(28),dp(25)));
        heroSource=text("  VOTRE MI TV",14,0xfff0e8ff);source.addView(heroSource);
        hero.addView(source);
        heroTitle=text("Votre écran. Votre univers.",37,Color.WHITE);
        heroTitle.setSingleLine(true);heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        hero.addView(heroTitle,new LinearLayout.LayoutParams(-1,dp(54)));
        heroDescription=text("Recherche des applications disponibles…",15,0xffe0d6ee);
        heroDescription.setMaxLines(2);heroDescription.setEllipsize(TextUtils.TruncateAt.END);
        hero.addView(heroDescription);
        heroOpen=nav("Ouvrir",() -> {AppEntry app=findApp(selectedPackage);if(app!=null)openApp(app);});
        heroOpen.setEnabled(false);heroOpen.setVisibility(View.INVISIBLE);
        LinearLayout.LayoutParams openSpace=new LinearLayout.LayoutParams(dp(130),dp(36));
        openSpace.setMargins(0,dp(12),0,0);hero.addView(heroOpen,openSpace);
        content.addView(hero,new LinearLayout.LayoutParams(-1,dp(heroHeight)));

        TextView quickLabel=text("À portée de télécommande",14,0xffe7dcf7);
        content.addView(quickLabel,new LinearLayout.LayoutParams(-1,dp(24)));
        HorizontalScrollView quickScroll=horizontal();quickRow=new LinearLayout(this);
        quickRow.setClipChildren(false);quickScroll.addView(quickRow);
        content.addView(quickScroll,new LinearLayout.LayoutParams(-1,dp(103)));
        appCount=text("Vos applications",14,0xffe7dcf7);
        appCount.setPadding(0,dp(8),0,0);
        content.addView(appCount,new LinearLayout.LayoutParams(-1,dp(30)));
        HorizontalScrollView appScroll=horizontal();appRow=new LinearLayout(this);
        appRow.setClipChildren(false);appScroll.addView(appRow);
        content.addView(appScroll,new LinearLayout.LayoutParams(-1,dp(112)));
        TextView hint=text("← →  Choisir   ·   OK  Ouvrir   ·   Menu  Personnaliser",11,0xffbfb0d3);
        hint.setPadding(0,dp(4),0,dp(4));content.addView(hint);
        setContentView(frame);homeNav.requestFocus();
    }
    private HorizontalScrollView horizontal() {
        HorizontalScrollView s=new HorizontalScrollView(this);s.setHorizontalScrollBarEnabled(false);
        s.setSmoothScrollingEnabled(true);s.setClipToPadding(false);s.setClipChildren(false);
        return s;
    }
    private TextView text(String s,int size,int color) {
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;
    }
    private GradientDrawable rounded(int color,int radius,int border,float stroke) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        if(stroke>0)d.setStroke(dp(stroke),border);return d;
    }
    private Button nav(String label,Runnable action) {
        Button b=new Button(this);b.setId(View.generateViewId());b.setText(label);b.setAllCaps(false);
        b.setTextSize(16);b.setPadding(dp(6),0,dp(6),0);b.setMinWidth(0);b.setMinimumWidth(0);
        b.setMinHeight(0);b.setMinimumHeight(0);b.setStateListAnimator(null);
        StateListDrawable bg=new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused},rounded(0xffeee9ff,36,0xffffffff,2));
        bg.addState(new int[]{android.R.attr.state_selected},rounded(0xffd8d0ef,36,0,0));
        bg.addState(new int[]{},rounded(0x204e376a,36,0,0));b.setBackground(bg);
        b.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused},{android.R.attr.state_selected},{}},
            new int[]{0xff19102c,0xff19102c,0xfff0eaff}));
        b.setOnClickListener(v -> action.run());return b;
    }
    private void selectTab(int index) {homeNav.setSelected(index==0);appsNav.setSelected(index==1);}
    private AppEntry findApp(String pkg) {for(AppEntry app:catalog)if(app.pkg.equals(pkg))return app;return null;}
    private void openApp(AppEntry app) {
        try {
            ActivityInfo activity=getPackageManager().getActivityInfo(app.intent.getComponent(),0);
            if(!activity.enabled || !activity.applicationInfo.enabled ||
               (activity.applicationInfo.flags & ApplicationInfo.FLAG_SUSPENDED)!=0)throw new ActivityNotFoundException();
            startActivity(new Intent(app.intent));
        } catch(PackageManager.NameNotFoundException | ActivityNotFoundException | SecurityException e) {
            message("Cette application n’est plus disponible. La liste va être actualisée.");refreshCatalog();
        }
    }
    private void refreshCatalog() {
        int generation=++catalogGeneration;
        images.execute(() -> {
            try {
                PackageManager pm=getPackageManager();Set<String> homes=new HashSet<>();
                Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
                for(ResolveInfo ri:pm.queryIntentActivities(home,0))if(ri.activityInfo!=null)homes.add(ri.activityInfo.packageName);
                List<AppCatalog.Candidate> candidates=new ArrayList<>();
                Map<String,ResolveInfo> activities=new HashMap<>();
                for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}) {
                    Intent main=new Intent(Intent.ACTION_MAIN).addCategory(category);
                    for(ResolveInfo ri:pm.queryIntentActivities(main,0)) {
                        ActivityInfo ai=ri.activityInfo;if(ai==null)continue;
                        String key=ai.packageName+"/"+ai.name;
                        CharSequence label=ri.loadLabel(pm);
                        candidates.add(new AppCatalog.Candidate(key,ai.packageName,label==null?ai.packageName:label.toString(),
                            ai.enabled && ai.applicationInfo.enabled,ai.exported,
                            (ai.applicationInfo.flags & ApplicationInfo.FLAG_SUSPENDED)!=0,
                            category.equals(Intent.CATEGORY_LEANBACK_LAUNCHER)));
                        activities.put(key,ri);
                    }
                }
                List<AppEntry> result=new ArrayList<>();
                for(AppCatalog.Candidate candidate:AppCatalog.select(candidates,homes,getPackageName())) {
                    ResolveInfo ri=activities.get(candidate.key);ActivityInfo ai=ri.activityInfo;
                    Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(candidate.tv?Intent.CATEGORY_LEANBACK_LAUNCHER:Intent.CATEGORY_LAUNCHER)
                        .setComponent(new ComponentName(ai.packageName,ai.name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    Drawable icon;try{icon=ri.loadIcon(pm);}catch(RuntimeException e){icon=pm.getDefaultActivityIcon();}
                    result.add(new AppEntry(candidate.pkg,candidate.label,launch,icon));
                }
                handler.post(()->{if(!isDestroyed() && generation==catalogGeneration)renderCatalog(result);});
            } catch(RuntimeException e) {
                handler.post(()->{if(!isDestroyed() && generation==catalogGeneration)
                    heroDescription.setText("Lecture des applications impossible. Revenez sur cet accueil pour réessayer.");});
            }
        });
    }
    private void renderCatalog(List<AppEntry> result) {
        StringBuilder signature=new StringBuilder();for(AppEntry app:result)signature.append(app.pkg).append('|').append(app.label).append(';');
        catalog=result;liveNav.setVisibility(findApp("com.mitv.livetv")==null?View.GONE:View.VISIBLE);
        if(signature.toString().equals(catalogSignature) && !initialFocus)return;
        catalogSignature=signature.toString();quickRow.removeAllViews();appRow.removeAllViews();firstApp=null;firstQuick=null;
        appCount.setText("Vos applications · "+result.size());
        for(int i=0;i<result.size();i++) {
            AppEntry app=result.get(i);
            LinearLayout circle=appCircle(app);LinearLayout.LayoutParams slot=new LinearLayout.LayoutParams(dp(94),dp(104));
            slot.setMargins(0,dp(4),dp(12),0);appRow.addView(circle,slot);if(firstApp==null)firstApp=circle;
            if(i<4) {
                LinearLayout card=quickCard(app,i);LinearLayout.LayoutParams cardSpace=new LinearLayout.LayoutParams(dp(202),dp(86));
                cardSpace.setMargins(0,dp(6),dp(16),dp(6));quickRow.addView(card,cardSpace);if(firstQuick==null)firstQuick=card;
            }
        }
        if(result.isEmpty()) {
            heroSource.setText("VOTRE MI TV");heroIcon.setImageDrawable(null);heroTitle.setText("Votre écran vous attend.");
            heroDescription.setText("Aucune application disponible à ouvrir. Les applications désactivées restent masquées.");
            heroOpen.setEnabled(false);heroOpen.setVisibility(View.INVISIBLE);
        } else {
            AppEntry selected=findApp(selectedPackage);showHero(selected==null?result.get(0):selected);
            if(firstQuick!=null)heroOpen.setNextFocusDownId(firstQuick.getId());
            if(firstApp!=null)appsNav.setNextFocusDownId(firstApp.getId());
        }
        if(initialFocus){initialFocus=false;homeNav.requestFocus();}
    }
    private void showHero(AppEntry app) {
        selectedPackage=app.pkg;heroSource.setText("  "+app.label);heroIcon.setImageDrawable(copyIcon(app));
        heroTitle.setText(app.label);
        heroDescription.setText(app.pkg.equals("com.google.android.youtube.tv")?"Vos vidéos, vos abonnements et votre historique dans YouTube.":
            app.pkg.equals("com.mitv.livetv")?"Retrouvez vos chaînes en direct sur votre Mi TV.":"Disponible sur votre Mi TV · Ouvrez votre application.");
        heroOpen.setEnabled(true);heroOpen.setVisibility(View.VISIBLE);heroOpen.setContentDescription("Ouvrir "+app.label);
    }
    private Drawable copyIcon(AppEntry app) {
        Drawable.ConstantState state=app.icon.getConstantState();return state==null?app.icon:state.newDrawable(getResources()).mutate();
    }
    private LinearLayout quickCard(AppEntry app,int index) {
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14),dp(10),dp(12),dp(10));card.setId(View.generateViewId());card.setFocusable(true);card.setClickable(true);
        int[][] colors={{0xff683366,0xff28204e},{0xff21647a,0xff20284b},{0xff624f86,0xff29203f},{0xff695343,0xff352147}};
        GradientDrawable base=new GradientDrawable(GradientDrawable.Orientation.TL_BR,colors[index%4]);base.setCornerRadius(dp(16));
        GradientDrawable focus=new GradientDrawable(GradientDrawable.Orientation.TL_BR,colors[index%4]);focus.setCornerRadius(dp(16));focus.setStroke(dp(3),0xffeee4ff);
        StateListDrawable bg=new StateListDrawable();bg.addState(new int[]{android.R.attr.state_focused},focus);bg.addState(new int[]{},base);card.setBackground(bg);
        ImageView icon=new ImageView(this);icon.setImageDrawable(copyIcon(app));icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView label=text(app.label,17,Color.WHITE);label.setMaxLines(2);label.setEllipsize(TextUtils.TruncateAt.END);label.setPadding(dp(10),0,0,0);
        card.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        card.setContentDescription(app.label+". Appuyez sur OK pour ouvrir.");
        card.setOnFocusChangeListener((v,focusState)->{animateFocus(v,focusState,1.035f);if(focusState){showHero(app);selectTab(0);}});
        card.setOnClickListener(v->openApp(app));return card;
    }
    private LinearLayout appCircle(AppEntry app) {
        LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        tile.setId(View.generateViewId());tile.setFocusable(true);tile.setClickable(true);
        FrameLayout disk=new FrameLayout(this);disk.setBackground(rounded(0xffeeebf5,100,0,0));disk.setClipToOutline(true);
        ImageView icon=new ImageView(this);icon.setImageDrawable(copyIcon(app));icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams art=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.CENTER);disk.addView(icon,art);
        tile.addView(disk,new LinearLayout.LayoutParams(dp(70),dp(70)));
        TextView label=text(app.label,12,0xffe1d8f2);label.setGravity(Gravity.CENTER);label.setSingleLine(true);label.setEllipsize(TextUtils.TruncateAt.END);
        label.setPadding(0,dp(6),0,0);tile.addView(label,new LinearLayout.LayoutParams(-1,-2));
        tile.setContentDescription(app.label+". Appuyez sur OK pour ouvrir.");
        tile.setOnFocusChangeListener((v,focusState)->{
            animateFocus(v,focusState,1.07f);disk.setBackground(rounded(0xffeeebf5,100,focusState?0xffd2bcff:0x00000000,focusState?3:0));
            label.setTextColor(focusState?Color.WHITE:0xffe1d8f2);if(focusState){showHero(app);selectTab(1);}
        });
        tile.setOnClickListener(v->openApp(app));return tile;
    }
    private void animateFocus(View v,boolean focused,float scale) {
        v.animate().scaleX(focused?scale:1).scaleY(focused?scale:1).setDuration(120).setInterpolator(new DecelerateInterpolator()).start();
    }
    private void searchApps() {
        if(catalog.isEmpty()){message("Aucune application disponible à rechercher.");return;}
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(20),0,dp(20),0);
        EditText query=new EditText(this);query.setHint("Nom de l’application");query.setSingleLine(true);panel.addView(query);
        ListView list=new ListView(this);panel.addView(list,new LinearLayout.LayoutParams(-1,dp(240)));
        List<AppEntry> matches=new ArrayList<>(catalog);List<String> names=new ArrayList<>();for(AppEntry a:matches)names.add(a.label);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,names);list.setAdapter(adapter);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Rechercher sur votre Mi TV").setView(panel).setNegativeButton("Fermer",null).create();
        query.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                matches.clear();adapter.clear();String q=s.toString().toLowerCase(Locale.FRANCE);
                for(AppEntry a:catalog)if(a.label.toLowerCase(Locale.FRANCE).contains(q)){matches.add(a);adapter.add(a.label);}adapter.notifyDataSetChanged();
            }
            public void afterTextChanged(Editable e){}
        });
        list.setOnItemClickListener((p,v,position,id)->{AppEntry chosen=matches.get(position);dialog.dismiss();openApp(chosen);});
        dialog.show();
    }
    private void options() {
        String[] items = {"Choisir une photo", "Retirer la photo", "Réglages de la TV", "Choisir l’accueil par défaut", "À propos"};
        new AlertDialog.Builder(this).setTitle("Votre accueil")
            .setItems(items, (d, which) -> {
                if (which == 0) {
                    Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    pick.setType("image/*"); pick.addCategory(Intent.CATEGORY_OPENABLE);
                    try { startActivityForResult(pick, PHOTO); }
                    catch (ActivityNotFoundException e) {
                        message("Aucun sélecteur de photos sur cette TV. Utilisez le transfert ADB décrit dans le guide.");
                    }
                } else if (which == 1) {
                    new AlertDialog.Builder(this).setMessage("Retirer uniquement la copie du fond d’écran ?")
                        .setNegativeButton("Annuler", null).setPositiveButton("Retirer", (x, y) -> {
                            imageGeneration++;
                            File local = new File(getFilesDir(), "background.jpg");
                            File external = externalPhoto();
                            if ((local.exists() && !local.delete()) ||
                                (external != null && external.exists() && !external.delete())) {
                                message("Impossible de retirer une copie. Réessayez via ADB.");
                            }
                            loadedFile = null; loadedTime = -1;
                            backdrop.photo = null; backdrop.invalidate();
                        }).show();
                } else if (which == 2) {
                    openSettings(Settings.ACTION_SETTINGS);
                } else if (which == 3) {
                    openSettings(Settings.ACTION_HOME_SETTINGS);
                } else {
                    message("Barça Home 1.3 · Mi TV Home\nSans publicité ni accès réseau.\nL’historique se consulte dans YouTube → Vous / Bibliothèque.\nAucune application système n’est désactivée par ce lanceur.");
                }
            }).show();
    }
    private void openSettings(String action) {
        try { startActivity(new Intent(action)); }
        catch (ActivityNotFoundException e) { message("Cette page n’est pas disponible sur votre TV. Utilisez les réglages Xiaomi."); }
    }
    private void message(String s) { new AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK", null).show(); }
    private File externalPhoto() {
        File dir = getExternalFilesDir(null);
        return dir == null ? null : new File(dir, "background.jpg");
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PHOTO || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        images.execute(() -> {
            File temp = new File(getFilesDir(), "background.tmp");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(temp)) {
                if (in == null) throw new IOException();
                byte[] buf = new byte[8192]; int count; long total = 0;
                while ((count = in.read(buf)) != -1) {
                    total += count;
                    if (total > 30L * 1024 * 1024) throw new IOException("Photo trop volumineuse");
                    out.write(buf, 0, count);
                }
            } catch (IOException | SecurityException e) {
                temp.delete(); handler.post(() -> message("Lecture impossible. Choisissez une image JPG ou PNG de moins de 30 Mo.")); return;
            }
            Bitmap check = decode(temp);
            if (check == null) { temp.delete(); handler.post(() -> message("Format d’image non reconnu.")); return; }
            check.recycle();
            File dest = new File(getFilesDir(), "background.jpg");
            if (!temp.renameTo(dest)) { temp.delete(); handler.post(() -> message("Enregistrement impossible.")); return; }
            // The most recently saved photo takes precedence.
            getPreferences(0).edit().putBoolean("useInternal", true).apply();
            handler.post(this::loadPhoto);
        });
    }
    private Bitmap decode(File f) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options(); opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), opts);
            if (opts.outWidth < 1 || opts.outHeight < 1) return null;
            opts.inSampleSize = 1;
            while (opts.outWidth / opts.inSampleSize > 1920 || opts.outHeight / opts.inSampleSize > 1080) opts.inSampleSize *= 2;
            opts.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(f.getPath(), opts);
        } catch (RuntimeException | OutOfMemoryError e) { return null; }
    }
    private void loadPhoto() {
        File internal = new File(getFilesDir(), "background.jpg");
        File external = externalPhoto();
        File chosen = external != null && external.exists() &&
            (!internal.exists() || external.lastModified() > internal.lastModified()) ? external : internal;
        if (!chosen.exists()) return;
        long modified = chosen.lastModified();
        if (chosen.equals(loadedFile) && modified == loadedTime) return;
        int generation = ++imageGeneration;
        images.execute(() -> {
            Bitmap b = decode(chosen);
            handler.post(() -> {
                if (isDestroyed() || generation != imageGeneration) { if (b != null) b.recycle(); return; }
                if (b == null) { message("Photo illisible. Le fond par défaut reste disponible."); return; }
                loadedFile = chosen; loadedTime = modified;
                backdrop.photo = b; backdrop.invalidate();
            });
        });
    }
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(tick); tick.run(); loadPhoto(); refreshCatalog(); }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
    @Override protected void onDestroy() { imageGeneration++; catalogGeneration++; handler.removeCallbacks(tick); images.shutdownNow(); super.onDestroy(); }
    @Override public void onBackPressed() { contentScroll.smoothScrollTo(0,0);homeNav.requestFocus(); }
    @Override public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU) { options(); return true; }
        return super.onKeyUp(keyCode, event);
    }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private class NavMark extends Drawable {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final boolean settings;
        NavMark(boolean settings){this.settings=settings;}
        @Override public int getIntrinsicWidth(){return dp(22);}
        @Override public int getIntrinsicHeight(){return dp(22);}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
        @Override public void draw(Canvas c){
            Rect r=getBounds();int save=c.save();c.translate(r.left,r.top);c.scale(r.width()/24f,r.height()/24f);
            boolean focused=false;for(int state:getState())if(state==android.R.attr.state_focused)focused=true;
            paint.setColor(focused?0xff211433:0xffeee7ff);paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);
            if(settings){
                c.drawCircle(12,12,7,paint);c.drawCircle(12,12,2.5f,paint);
                for(int i=0;i<8;i++){double a=Math.PI*i/4;c.drawLine(12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,
                    12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10,paint);}
            }else{c.drawCircle(10,10,6.5f,paint);c.drawLine(15,15,21,21,paint);}
            c.restoreToCount(save);
        }
        @Override public void setAlpha(int a){paint.setAlpha(a);}
        @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private class Backdrop extends View {
        Bitmap photo;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final Path wave = new Path();
        Backdrop() { super(HomeActivity.this); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xff160a42,0xff30206c,0xff783d75},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint); paint.setShader(null);
            if (photo != null) {
                float scale = Math.max(w/photo.getWidth(), h/photo.getHeight());
                float bw = photo.getWidth()*scale, bh = photo.getHeight()*scale;
                c.drawBitmap(photo,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),paint);
            } else {
                // Architectural fallback; a personal photo is supplied locally.
                paint.setColor(0x488dc3b0);
                for (int i=0;i<5;i++) {
                    float x=w*(.62f+i*.055f), top=h*(.16f+Math.abs(2-i)*.065f);
                    wave.reset(); wave.moveTo(x,h*.86f); wave.lineTo(x+dp(7),top+dp(29));
                    wave.quadTo(x+dp(15),top-dp(26),x+dp(23),top+dp(29));
                    wave.lineTo(x+dp(33),h*.86f); wave.close(); c.drawPath(wave,paint);
                }
            }
            paint.setShader(new LinearGradient(0,0,w*.87f,0,new int[]{0xeb160b37,0x991b103f,0x181b103f},new float[]{0,.48f,1},Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint);
            paint.setShader(new LinearGradient(0,h*.40f,0,h,new int[]{0x00110b21,0xff110b21},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint); paint.setShader(null);
            // Low-cost static wave inspired by the city's organic facades.
            wave.reset(); wave.moveTo(w*.60f,h); wave.cubicTo(w*.77f,h*.81f,w*.86f,h*1.01f,w,h*.78f);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(0x70e8ca93);
            c.drawPath(wave,paint); paint.setStyle(Paint.Style.FILL);
        }
    }
}
