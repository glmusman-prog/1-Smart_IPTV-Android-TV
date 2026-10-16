package com.example.smartiptv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_M3U = 4102;
    private final ArrayList<Channel> channels = new ArrayList<>();
    private final Set<String> favorites = new HashSet<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private ExoPlayer player;
    private PlayerView playerView;
    private ListView channelList;
    private ArrayAdapter<String> adapter;
    private TextView nowPlaying, status;
    private EditText urlInput;
    private int currentIndex = -1;
    private String digitBuffer = "";
    private Runnable clearDigits;

    static class Channel {
        String name, url, group;
        int number;
        Channel(String n, String u, String g, int no) { name=n; url=u; group=g; number=no; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        player = new ExoPlayer.Builder(this).build();
        buildUi();
        restoreState();
        refreshList();
    }

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setOnClickListener(v -> action.run());
        b.setFocusable(true); b.setFocusableInTouchMode(true); return b;
    }
    private LinearLayout row() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12), dp(8), dp(12), dp(8)); root.setBackgroundColor(Color.rgb(9,21,34));
        TextView title = text("SMART IPTV TV  •  ریموٹ سے چینل نمبر ڈائل کریں", 21, Color.WHITE); title.setPadding(dp(4),0,0,dp(5)); root.addView(title, lp(-1,42));
        LinearLayout body = row(); root.addView(body, new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout left = new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL); left.setPadding(dp(4),dp(4),dp(10),dp(4)); body.addView(left,new LinearLayout.LayoutParams(0,-1,1.45f));
        playerView = new PlayerView(this); playerView.setPlayer(player); playerView.setUseController(true); left.addView(playerView,new LinearLayout.LayoutParams(-1,0,1));
        nowPlaying = text("کوئی چینل منتخب نہیں",19,Color.WHITE); nowPlaying.setPadding(0,dp(6),0,0); left.addView(nowPlaying,lp(-1,36));
        TextView hint = text("ریموٹ کے ہندسے دبائیں، پھر OK/Enter کریں۔",13,Color.rgb(175,194,212)); left.addView(hint,lp(-1,28));
        LinearLayout controls = row(); left.addView(controls,lp(-1,48));
        controls.addView(button("◀ پچھلا", () -> stepChannel(-1)),new LinearLayout.LayoutParams(0,-1,1));
        controls.addView(button("اگلا ▶", () -> stepChannel(1)),new LinearLayout.LayoutParams(0,-1,1));
        controls.addView(button("★ فیورٹ", this::toggleFavorite),new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout right = new LinearLayout(this); right.setOrientation(LinearLayout.VERTICAL); right.setPadding(dp(8),dp(4),dp(4),dp(4)); body.addView(right,new LinearLayout.LayoutParams(0,-1,1));
        urlInput = new EditText(this); urlInput.setSingleLine(true); urlInput.setTextSize(14); urlInput.setHint("M3U playlist URL"); urlInput.setHintTextColor(Color.GRAY); urlInput.setTextColor(Color.WHITE); urlInput.setSelectAllOnFocus(true); right.addView(urlInput,lp(-1,48));
        LinearLayout loadRow = row(); right.addView(loadRow,lp(-1,48));
        loadRow.addView(button("URL لوڈ", this::loadUrl),new LinearLayout.LayoutParams(0,-1,1));
        loadRow.addView(button("M3U فائل", () -> pickFile()),new LinearLayout.LayoutParams(0,-1,1));
        status = text("اپنی M3U فائل یا URL شامل کریں۔",12,Color.rgb(85,224,168)); right.addView(status,lp(-1,36));
        channelList = new ListView(this); channelList.setChoiceMode(ListView.CHOICE_MODE_SINGLE); channelList.setBackgroundColor(Color.rgb(16,36,58)); channelList.setDividerHeight(dp(1));
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, new ArrayList<>());
        channelList.setAdapter(adapter); right.addView(channelList,new LinearLayout.LayoutParams(-1,0,1));
        channelList.setOnItemClickListener((parent,view,position,id) -> playIndex(position));
        channelList.setOnItemLongClickListener((parent,view,position,id) -> { editChannel(position); return true; });
        TextView footer = text("OK: چلائیں  •  چینل پر دیر تک OK: نمبر تبدیل کریں  •  نمبر 1–9999",12,Color.LTGRAY); root.addView(footer,lp(-1,28));
        setContentView(root);
    }

    private void setStatus(String s) { status.setText(s); }
    private void refreshList() {
        Collections.sort(channels, Comparator.comparingInt(c -> c.number));
        adapter.clear();
        for (Channel c : channels) adapter.add(String.format("%04d   %s%s", c.number, favorites.contains(c.url) ? "★ " : "", c.name));
        adapter.notifyDataSetChanged();
        if (currentIndex >= 0 && currentIndex < channels.size()) channelList.setSelection(currentIndex);
        saveState();
    }

    private void playIndex(int index) {
        if (index < 0 || index >= channels.size()) return;
        currentIndex = index; Channel c = channels.get(index);
        nowPlaying.setText(String.format("CH %d  •  %s", c.number, c.name));
        player.setMediaItem(MediaItem.fromUri(c.url)); player.prepare(); player.play();
        setStatus("چینل منتخب: " + c.number); refreshList();
    }
    private void stepChannel(int delta) {
        if (channels.isEmpty()) return;
        int index = currentIndex < 0 ? 0 : (currentIndex + delta + channels.size()) % channels.size(); playIndex(index);
    }
    private void toggleFavorite() {
        if (currentIndex < 0 || currentIndex >= channels.size()) { setStatus("پہلے کوئی چینل منتخب کریں۔"); return; }
        Channel c = channels.get(currentIndex); if (!favorites.add(c.url)) favorites.remove(c.url);
        setStatus(favorites.contains(c.url) ? "فیورٹ میں شامل ہوگیا۔" : "فیورٹ سے ہٹا دیا گیا۔"); refreshList();
    }
    private void editChannel(int index) {
        if(index<0 || index>=channels.size()) return; Channel c=channels.get(index);
        EditText input=new EditText(this); input.setInputType(InputType.TYPE_CLASS_NUMBER); input.setSingleLine(true); input.setText(String.valueOf(c.number)); input.selectAll();
        new AlertDialog.Builder(this).setTitle("چینل نمبر تبدیل کریں: "+c.name).setMessage("1 سے 9999 تک منفرد نمبر درج کریں۔").setView(input)
          .setPositiveButton("محفوظ",(d,w)->{String s=input.getText().toString().trim();try{int n=Integer.parseInt(s);if(n<1||n>9999)throw new Exception();for(Channel other:channels)if(other!=c&&other.number==n){setStatus("یہ نمبر پہلے سے استعمال ہو رہا ہے۔");return;}c.number=n;refreshList();setStatus("چینل نمبر محفوظ ہوگیا۔");}catch(Exception ex){setStatus("درست نمبر 1 سے 9999 درج کریں۔");}})
          .setNeutralButton("فیورٹ تبدیل کریں",(d,w)->{if(!favorites.add(c.url))favorites.remove(c.url);refreshList();}).setNegativeButton("منسوخ",null).show();
    }

    private void loadUrl() {
        String address=urlInput.getText().toString().trim(); if(address.isEmpty()){setStatus("M3U URL درج کریں۔");return;}
        setStatus("پلے لسٹ لوڈ ہو رہی ہے…");
        io.execute(() -> { try { HttpURLConnection conn=(HttpURLConnection)new URL(address).openConnection();conn.setConnectTimeout(12000);conn.setReadTimeout(15000);conn.setRequestProperty("User-Agent","SmartIPTVTV/1.0");InputStream in=conn.getInputStream();String content=readAll(in);conn.disconnect();ArrayList<Channel> parsed=parseM3u(content);runOnUiThread(()->applyPlaylist(parsed)); } catch(Exception e){runOnUiThread(()->setStatus("URL لوڈ نہیں ہوا؛ M3U فائل منتخب کریں یا لنک چیک کریں۔"));} });
    }
    private void pickFile() { Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_M3U); }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==PICK_M3U&&resultCode==RESULT_OK&&data!=null){Uri uri=data.getData();io.execute(()->{try{InputStream in=getContentResolver().openInputStream(uri);String content=readAll(in);ArrayList<Channel> parsed=parseM3u(content);runOnUiThread(()->applyPlaylist(parsed));}catch(Exception e){runOnUiThread(()->setStatus("فائل نہیں پڑھی جا سکی۔"));}});}}
    private String readAll(InputStream in)throws Exception{StringBuilder sb=new StringBuilder();try(BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=br.readLine())!=null)sb.append(line).append('\n');}return sb.toString();}
    private ArrayList<Channel> parseM3u(String text){ArrayList<Channel> out=new ArrayList<>();String[] lines=text.replace("\uFEFF","").split("\\r?\\n");String name=null,group="";for(String raw:lines){String line=raw.trim();if(line.isEmpty())continue;if(line.startsWith("#EXTINF")){int comma=line.lastIndexOf(',');name=comma>=0?line.substring(comma+1).trim():"Channel "+(out.size()+1);group="";int gi=line.indexOf("group-title=\"");if(gi>=0){int st=gi+13,en=line.indexOf('"',st);if(en>st)group=line.substring(st,en);}continue;}if(line.startsWith("#"))continue;if(line.startsWith("http://")||line.startsWith("https://")){if(name==null)name="Channel "+(out.size()+1);out.add(new Channel(name,line,group,out.size()+1));name=null;group="";}}return out;}
    private void applyPlaylist(ArrayList<Channel> parsed){if(parsed.isEmpty()){setStatus("پلے لسٹ میں کوئی قابلِ شناخت چینل نہیں ملا۔");return;}channels.clear();channels.addAll(parsed);currentIndex=-1;refreshList();setStatus(channels.size()+" چینلز لوڈ ہوگئے۔");if(!channels.isEmpty())channelList.requestFocus();}

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        int digit=-1;
        if(keyCode>=KeyEvent.KEYCODE_0&&keyCode<=KeyEvent.KEYCODE_9)digit=keyCode-KeyEvent.KEYCODE_0;
        else if(keyCode>=KeyEvent.KEYCODE_NUMPAD_0&&keyCode<=KeyEvent.KEYCODE_NUMPAD_9)digit=keyCode-KeyEvent.KEYCODE_NUMPAD_0;
        if(digit>=0){if(digitBuffer.length()<4)digitBuffer+=digit;setStatus("چینل نمبر: "+digitBuffer+"  •  OK دبائیں");if(clearDigits!=null)handler.removeCallbacks(clearDigits);clearDigits=()->digitBuffer="";handler.postDelayed(clearDigits,6000);return true;}
        if(keyCode==KeyEvent.KEYCODE_ENTER||keyCode==KeyEvent.KEYCODE_DPAD_CENTER||keyCode==KeyEvent.KEYCODE_NUMPAD_ENTER){if(!digitBuffer.isEmpty()){String requested=digitBuffer;digitBuffer="";if(clearDigits!=null)handler.removeCallbacks(clearDigits);try{int n=Integer.parseInt(requested);if(n<1||n>9999){setStatus("چینل نمبر 1 سے 9999 تک ہونا چاہیے۔");return true;}for(int i=0;i<channels.size();i++)if(channels.get(i).number==n){playIndex(i);return true;}setStatus("چینل نمبر "+n+" موجود نہیں۔");}catch(Exception e){setStatus("چینل نمبر درست نہیں۔");}return true;}}
        if(keyCode==KeyEvent.KEYCODE_DPAD_UP&&digitBuffer.isEmpty()&&currentIndex>=0){stepChannel(-1);return true;}
        if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN&&digitBuffer.isEmpty()&&currentIndex>=0){stepChannel(1);return true;}
        return super.onKeyDown(keyCode,event);
    }

    private void saveState(){try{JSONArray arr=new JSONArray();for(Channel c:channels){JSONObject o=new JSONObject();o.put("name",c.name);o.put("url",c.url);o.put("group",c.group);o.put("number",c.number);o.put("fav",favorites.contains(c.url));arr.put(o);}getPreferences(MODE_PRIVATE).edit().putString("channels",arr.toString()).apply();}catch(Exception ignored){}}
    private void restoreState(){try{String s=getPreferences(MODE_PRIVATE).getString("channels","[]");JSONArray arr=new JSONArray(s);for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);Channel c=new Channel(o.optString("name"),o.optString("url"),o.optString("group"),o.optInt("number",i+1));channels.add(c);if(o.optBoolean("fav"))favorites.add(c.url);}}catch(Exception ignored){}}
    @Override protected void onDestroy(){super.onDestroy();if(player!=null)player.release();io.shutdownNow();}
}
