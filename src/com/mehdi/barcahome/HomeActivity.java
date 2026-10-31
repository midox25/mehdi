package com.mehdi.barcahome;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Local-only TV launcher. No network, account, accessibility or device-admin access. */
public class HomeActivity extends Activity {
    private static final int PHOTO = 7;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService images = Executors.newSingleThreadExecutor();
    private TextView clock;
    private Backdrop backdrop;
    private File loadedFile;
    private long loadedTime = -1;
    private int imageGeneration;
    private final Runnable tick = new Runnable() {
        public void run() {
            clock.setText(new SimpleDateFormat("HH:mm", Locale.FRANCE).format(new Date()));
            handler.postDelayed(this, 15000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(1024, 1024);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        FrameLayout frame = new FrameLayout(this);
        backdrop = new Backdrop();
        frame.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(48), dp(28), dp(48), dp(28));
        frame.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("B A R C E L O N A   /   C A S A", 15, 0xfff1d3a2);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));
        clock = text("", 24, Color.WHITE);
        header.addView(clock);
        page.addView(header);

        TextView title = text("Le spectacle commence ici.", 26, 0xfffff3de);
        title.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        title.setPadding(0, dp(6), 0, dp(2));
        page.addView(title);
        TextView subtitle = text("Votre Camp Nou · Choisissez votre application", 13, 0xffead9bd);
        page.addView(subtitle);

        FrameLayout arena = new FrameLayout(this);
        arena.setClipChildren(false);
        StadiumView stadium = new StadiumView();
        arena.addView(stadium, new FrameLayout.LayoutParams(-1, -1));
        page.addView(arena, new LinearLayout.LayoutParams(-1, 0, 1));
        page.setClipChildren(false);

        LinearLayout apps = new LinearLayout(this);
        apps.setGravity(Gravity.CENTER);
        apps.setClipChildren(false);
        LinearLayout youtube = appIcon("YouTube", true, stadium, 0,
            () -> launch("com.google.android.youtube.tv", "YouTube"));
        LinearLayout live = appIcon("Live TV", false, stadium, 1,
            () -> launch("com.mitv.livetv", "Live TV"));
        youtube.setId(View.generateViewId()); live.setId(View.generateViewId());
        youtube.setNextFocusLeftId(live.getId()); youtube.setNextFocusRightId(live.getId());
        live.setNextFocusLeftId(youtube.getId()); live.setNextFocusRightId(youtube.getId());
        LinearLayout.LayoutParams iconSpace = new LinearLayout.LayoutParams(dp(110), dp(98));
        iconSpace.setMargins(dp(12), dp(8), dp(12), dp(8));
        apps.addView(youtube, iconSpace);
        LinearLayout.LayoutParams liveSpace = new LinearLayout.LayoutParams(dp(110), dp(98));
        liveSpace.setMargins(dp(12), dp(8), dp(12), dp(8));
        apps.addView(live, liveSpace);
        FrameLayout.LayoutParams middle = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        arena.addView(apps, middle);

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        TextView hint = text("← →  Changer d’application    ·    OK  Ouvrir    ·    ↓  Réglages", 12, 0xffead9bd);
        footer.addView(hint, new LinearLayout.LayoutParams(0, dp(54), 1));
        Button config = tile("Personnaliser", 0xff3d4136, this::options);
        config.setTextSize(14);
        footer.addView(config, new LinearLayout.LayoutParams(dp(174), dp(44)));
        config.setId(View.generateViewId());
        youtube.setNextFocusDownId(config.getId()); live.setNextFocusDownId(config.getId());
        config.setNextFocusUpId(youtube.getId());
        stadium.onSelection = index -> config.setNextFocusUpId(index == 0 ? youtube.getId() : live.getId());
        page.addView(footer);
        setContentView(frame);
        youtube.requestFocus();
    }

    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("kern");
        return t;
    }
    private LinearLayout appIcon(String label, boolean youtube, StadiumView stadium, int index, Runnable action) {
        LinearLayout icon = new LinearLayout(this);
        icon.setOrientation(LinearLayout.VERTICAL); icon.setGravity(Gravity.CENTER);
        icon.setPadding(dp(8), dp(8), dp(8), dp(6));
        icon.setFocusable(true); icon.setClickable(true);
        icon.setContentDescription(label + ". Appuyez sur OK pour ouvrir.");
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(0xee182c32); normal.setCornerRadius(dp(20)); normal.setStroke(dp(1),0x807fa492);
        GradientDrawable focus = new GradientDrawable();
        focus.setColor(0xff243f43); focus.setCornerRadius(dp(20)); focus.setStroke(dp(3),0xffffd795);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused},focus);
        background.addState(new int[]{},normal);
        icon.setBackground(background);
        ImageView mark = new ImageView(this);
        mark.setImageDrawable(new AppMark(youtube));
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        icon.addView(mark,new LinearLayout.LayoutParams(dp(54),dp(40)));
        TextView name = text(label,17,Color.WHITE);
        name.setGravity(Gravity.CENTER); name.setPadding(0,dp(5),0,0);
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        icon.addView(name);
        icon.setOnFocusChangeListener((v,focused) -> {
            v.animate().scaleX(focused ? 1.07f : 1f).scaleY(focused ? 1.07f : 1f)
                .setDuration(120).setInterpolator(new DecelerateInterpolator()).start();
            if (focused) stadium.select(index);
        });
        icon.setOnClickListener(v -> action.run());
        return icon;
    }

    private Button tile(String label, int color, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(21); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        b.setFocusable(true); b.setStateListAnimator(null);
        b.setBackground(new CeramicTile(color));
        b.setOnFocusChangeListener((v, focus) -> v.animate()
            .scaleX(focus ? 1.025f : 1f).scaleY(focus ? 1.025f : 1f)
            .setDuration(120).setInterpolator(new DecelerateInterpolator()).start());
        b.setOnClickListener(v -> action.run());
        return b;
    }
    private void launch(String pkg, String name) {
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) { message(name + " est indisponible. Vérifiez que l’application est activée."); return; }
        try { startActivity(i); }
        catch (RuntimeException e) { message("Impossible d’ouvrir " + name + ". Aucun réglage n’a été changé."); }
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
                    message("Barça Home 1.2 · Camp Nou\nSans publicité ni accès réseau.\nL’historique se consulte dans YouTube → Vous / Bibliothèque.\nAucune application système n’est désactivée par ce lanceur.");
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
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(tick); tick.run(); loadPhoto(); }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
    @Override protected void onDestroy() { imageGeneration++; handler.removeCallbacks(tick); images.shutdownNow(); super.onDestroy(); }
    @Override public void onBackPressed() { /* Home stays on screen; no exit loop. */ }
    @Override public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU) { options(); return true; }
        return super.onKeyUp(keyCode, event);
    }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    /** Original lightweight pictograms; labels remain visible at all times. */
    private class AppMark extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean youtube;
        AppMark(boolean youtube) { this.youtube = youtube; }
        @Override public void draw(Canvas c) {
            Rect b=getBounds(); int save=c.save(); c.translate(b.left,b.top); c.scale(b.width()/64f,b.height()/48f);
            p.setStyle(Paint.Style.FILL);
            if (youtube) {
                p.setColor(0xffff0033); c.drawRoundRect(0,4,64,44,11,11,p);
                Path play=new Path(); play.moveTo(26,14);play.lineTo(26,34);play.lineTo(44,24);play.close();
                p.setColor(Color.WHITE);c.drawPath(play,p);
            } else {
                p.setColor(0xff78c9bd);c.drawRoundRect(1,3,63,41,8,8,p);
                p.setColor(0xff173e45);c.drawRoundRect(6,8,58,36,4,4,p);
                p.setColor(0xffffedc9);c.drawRect(22,44,42,47,p);
                p.setStrokeWidth(2);c.drawLine(32,40,32,45,p);
                p.setColor(0xff78c9bd);p.setStrokeWidth(2.5f);
                c.drawLine(15,27,23,19,p);c.drawLine(23,19,32,27,p);c.drawLine(32,27,47,16,p);
            }
            c.restoreToCount(save);
        }
        @Override public void setAlpha(int a) { p.setAlpha(a); }
        @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
    /** Stylized Camp Nou bowl, painted once; no live rendering or scrolling effects. */
    private class StadiumView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int selected;
        java.util.function.IntConsumer onSelection;
        StadiumView() { super(HomeActivity.this); setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
        void select(int index) { selected=index; invalidate(); if(onSelection!=null)onSelection.accept(index); }
        @Override protected void onDraw(Canvas c) {
            float w=getWidth(),h=getHeight();
            float bowlW=Math.min(w*.91f,h*2.65f),bowlH=h*.90f;
            float left=(w-bowlW)/2,top=(h-bowlH)/2;
            RectF outer=new RectF(left,top,left+bowlW,top+bowlH);
            p.setStyle(Paint.Style.FILL);p.setColor(0x99071116);
            c.drawOval(new RectF(left-dp(7),top+dp(6),left+bowlW+dp(7),top+bowlH+dp(12)),p);
            int[] tierColors={0xffc6a775,0xff253b52,0xff773b4e,0xff243b56,0xff92404c,0xff2a4560};
            for(int i=0;i<tierColors.length;i++) {
                float insetX=bowlW*.028f*i,insetY=bowlH*.037f*i;
                p.setColor(tierColors[i]);c.drawOval(new RectF(left+insetX,top+insetY,left+bowlW-insetX,top+bowlH-insetY),p);
            }
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.7f));p.setColor(0x70e4c9a0);
            for(int i=1;i<5;i++) {
                float ix=bowlW*.028f*i,iy=bowlH*.037f*i;
                c.drawOval(new RectF(left+ix,top+iy,left+bowlW-ix,top+bowlH-iy),p);
            }
            // Aisles define the recognizable oval seating bowl.
            for(int angle=0;angle<360;angle+=10) {
                double rad=Math.toRadians(angle);
                float x1=w/2+(float)Math.cos(rad)*bowlW*.475f;
                float y1=h/2+(float)Math.sin(rad)*bowlH*.475f;
                float x2=w/2+(float)Math.cos(rad)*bowlW*.325f;
                float y2=h/2+(float)Math.sin(rad)*bowlH*.28f;
                c.drawLine(x1,y1,x2,y2,p);
            }
            RectF field=new RectF(w/2-bowlW*.315f,h/2-bowlH*.255f,w/2+bowlW*.315f,h/2+bowlH*.255f);
            p.setStyle(Paint.Style.FILL);p.setColor(0xff174d3e);c.drawRoundRect(field,dp(9),dp(9),p);
            int save=c.save();c.clipRect(field);
            p.setColor(0x284ca77a);
            for(int stripe=0;stripe<12;stripe+=2) {
                float x=field.left+field.width()*stripe/12;
                c.drawRect(x,field.top,x+field.width()/12,field.bottom,p);
            }
            c.restoreToCount(save);
            p.setStyle(Paint.Style.STROKE);p.setColor(0x99e8ebd6);p.setStrokeWidth(dp(1));
            RectF lines=new RectF(field);lines.inset(dp(6),dp(6));
            c.drawRect(lines,p);c.drawLine(w/2,lines.top,w/2,lines.bottom,p);
            c.drawCircle(w/2,h/2,field.height()*.24f,p);
            c.drawRect(lines.left, h/2-field.height()*.26f,lines.left+field.width()*.13f,h/2+field.height()*.26f,p);
            c.drawRect(lines.right-field.width()*.13f,h/2-field.height()*.26f,lines.right,h/2+field.height()*.26f,p);
            p.setStyle(Paint.Style.FILL);p.setColor(0xffffe0a7);p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(dp(12));
            c.drawText("C A M P   N O U",w/2,top+bowlH*.15f,p);
            p.setTextSize(dp(10));p.setColor(0xffe8ceab);
            c.drawText(selected==0?"YOUTUBE  ·  VIDÉOS & HISTORIQUE":"LIVE TV  ·  VOS CHAÎNES EN DIRECT",w/2,top+bowlH*.88f,p);
        }
    }
    /** Rounded asymmetric ceramic silhouette with a small trencadis mosaic. */
    private class CeramicTile extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shape = new Path();
        private final int color;
        CeramicTile(int color) { this.color = color; }
        @Override public boolean isStateful() { return true; }
        @Override protected boolean onStateChange(int[] state) { invalidateSelf(); return true; }
        @Override public void draw(Canvas c) {
            Rect r = getBounds(); float w = r.width(), h = r.height(), inset = dp(3);
            shape.reset();
            boolean compact = h < dp(70);
            if (compact) {
                shape.addRoundRect(new RectF(inset,inset,w-inset,h-inset),dp(18),dp(18),Path.Direction.CW);
            } else {
            shape.moveTo(inset + dp(26), inset + dp(3));
            shape.cubicTo(w*.45f, inset + dp(8), w*.69f, inset, w-dp(29), inset);
            shape.quadTo(w-inset, inset, w-inset, dp(31));
            shape.lineTo(w-inset, h-dp(26));
            shape.quadTo(w-inset, h-inset, w-dp(33), h-inset);
            shape.cubicTo(w*.65f,h-dp(9),w*.33f,h-inset,dp(27),h-inset);
            shape.quadTo(inset,h-inset,inset,h-dp(28));
            shape.lineTo(inset,dp(31));
            shape.quadTo(inset,inset,dp(29),inset+dp(3)); shape.close();
            }
            boolean focused = false;
            for (int state : getState()) if (state == android.R.attr.state_focused) focused = true;
            p.setStyle(Paint.Style.FILL); p.setColor(color); c.drawPath(shape,p);
            int save = c.save(); c.clipPath(shape);
            int[] colors = {0x807fb3a7,0x80eacb93,0x80bd6d47,0x806c989c};
            for (int i=0; !compact && i<9; i++) {
                float x=w-dp(48)+(i%3)*dp(14), y=dp(9)+(i/3)*dp(14);
                Path chip = new Path(); chip.moveTo(x,y); chip.lineTo(x+dp(11),y+dp(2));
                chip.lineTo(x+dp(8),y+dp(12)); chip.lineTo(x-dp(2),y+dp(9)); chip.close();
                p.setColor(colors[i%4]); c.drawPath(chip,p);
            }
            c.restoreToCount(save);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(focused ? 2.5f : 1));
            p.setColor(focused ? 0xffffd795 : 0x70ead9bd); c.drawPath(shape,p);
            p.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int alpha) { p.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
    private class Backdrop extends View {
        Bitmap photo;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final Path wave = new Path();
        Backdrop() { super(HomeActivity.this); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xff213c42,0xff517b79,0xffaa7357},null,Shader.TileMode.CLAMP));
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
            paint.setShader(new LinearGradient(0,0,w*.87f,0,new int[]{0xe51c2523,0x991c2523,0x081c2523},new float[]{0,.48f,1},Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint);
            paint.setShader(new LinearGradient(0,h*.40f,0,h,new int[]{0x001c2523,0xd91c2523},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint); paint.setShader(null);
            // Low-cost static wave inspired by the city's organic facades.
            wave.reset(); wave.moveTo(w*.60f,h); wave.cubicTo(w*.77f,h*.81f,w*.86f,h*1.01f,w,h*.78f);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(0x70e8ca93);
            c.drawPath(wave,paint); paint.setStyle(Paint.Style.FILL);
        }
    }
}
