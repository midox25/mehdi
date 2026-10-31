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
        TextView brand = text("B A R Ç A  /  H O M E", 16, 0xffe7c58b);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));
        clock = text("", 24, Color.WHITE);
        header.addView(clock);
        page.addView(header);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.addView(text("Votre salon.\nTout simplement.", 42, Color.WHITE));
        TextView sub = text("Vos vidéos et vos chaînes, à portée de télécommande.", 17, 0xffc1cad9);
        sub.setPadding(0, dp(14), 0, 0);
        hero.addView(sub);
        page.addView(hero, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout row = new LinearLayout(this);
        row.setClipChildren(false);
        page.setClipChildren(false);
        Button youtube = tile("▶   YouTube\nVidéos & historique", 0xffbb263d,
            () -> launch("com.google.android.youtube.tv", "YouTube"));
        Button live = tile("▣   Live TV\nVos chaînes en direct", 0xff254e75,
            () -> launch("com.mitv.livetv", "Live TV"));
        LinearLayout.LayoutParams card = new LinearLayout.LayoutParams(0, dp(102), 1);
        card.setMargins(0, dp(8), dp(20), dp(8));
        row.addView(youtube, card);
        LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, dp(102), 1);
        second.setMargins(0, dp(8), 0, dp(8));
        row.addView(live, second);
        page.addView(row);

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        TextView hint = text("Flèches pour naviguer · OK pour ouvrir", 13, 0xffc1cad9);
        footer.addView(hint, new LinearLayout.LayoutParams(0, dp(54), 1));
        Button config = tile("Personnaliser", 0xff17273e, this::options);
        config.setTextSize(14);
        footer.addView(config, new LinearLayout.LayoutParams(dp(174), dp(44)));
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
    private Button tile(String label, int color, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(21); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        b.setFocusable(true); b.setStateListAnimator(null);
        GradientDrawable idle = new GradientDrawable();
        idle.setColor(color); idle.setCornerRadius(dp(16));
        idle.setStroke(dp(1), 0x446d829c);
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(color); focused.setCornerRadius(dp(16));
        focused.setStroke(dp(3), 0xfff1d39c);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, idle);
        b.setBackground(states);
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
                    message("Barça Home 1.0\nSans publicité ni accès réseau.\nL’historique se consulte dans YouTube → Vous / Bibliothèque.\nAucune application système n’est désactivée par ce lanceur.");
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
            // A selected photo takes precedence over an ADB-supplied photo.
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
        File chosen = (getPreferences(0).getBoolean("useInternal", false) && internal.exists()) ? internal :
            (external != null && external.exists() ? external : internal);
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
    private class Backdrop extends View {
        Bitmap photo; final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Backdrop() { super(HomeActivity.this); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xff091c35,0xff183c59,0xff6c344f},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint); paint.setShader(null);
            if (photo != null) {
                float scale = Math.max(w/photo.getWidth(), h/photo.getHeight());
                float bw = photo.getWidth()*scale, bh = photo.getHeight()*scale;
                c.drawBitmap(photo,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),paint);
            } else {
                // Quiet architectural silhouette, not a photograph.
                paint.setColor(0x243d7990);
                for (int i=0;i<18;i++) {
                    float x=w*.49f+i*w*.032f;
                    float top=h*(.40f+(i%4)*.055f);
                    c.drawRect(x,top,x+w*.025f,h,paint);
                }
                paint.setColor(0x33d5a16d);
                c.drawCircle(w*.81f,h*.25f,h*.075f,paint);
            }
            paint.setShader(new LinearGradient(0,0,w,0,new int[]{0xe6091426,0x92091426,0x40091426},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint);
            paint.setShader(new LinearGradient(0,h*.4f,0,h,new int[]{0x00091426,0xe6091426},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint); paint.setShader(null);
        }
    }
}
