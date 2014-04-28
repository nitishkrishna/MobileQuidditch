package myapp.mobilequidditch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity{

  private static final String TAG = "NitishKrishna";
  private static final ImageButton NULL = null;
  private boolean mWriteMode = false;
  NfcAdapter mNfcAdapter;
  static ImageView mNote;
  static ImageView ball;
  boolean haveball;
  boolean listen_flag;
  static Animation animation;
  int rssiValue;
  static String ballReturn;
  static boolean mResumed;
  static int netId;
  WifiManager wifiManager;
  CountDownTimer cTimer = null;
  ConnectivityManager cm;
  NetworkInfo netInfo;
  PendingIntent mNfcPendingIntent;
  IntentFilter[] mWriteTagFilters;
  IntentFilter[] mNdefExchangeFilters;
  CountDownTimer tempTimer;
  boolean gk=false;
  Handler handler;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    //Network Configuration
    
    /*WifiManager wifiManager = (WifiManager) MainActivity.this.getSystemService(Context.WIFI_SERVICE); 
    wifiManager.setWifiEnabled(true);
    String networkSSID = "skanda";
    String networkPass = "nitishkrishna";
    WifiConfiguration conf = new WifiConfiguration();
    conf.SSID = "\"" + networkSSID + "\""; 
    conf.preSharedKey  = "\"" + networkPass + "\"";
    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
    conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
    netId = wifiManager.addNetwork(conf);*/

    //Set Initial Game State
    
    setContentView(R.layout.activity_main);
    System.out.println("In On Create");
    mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    TextView sly_score = (TextView) findViewById(R.id.Sly_score);
    TextView gryff_score = (TextView) findViewById(R.id.Gryff_score);
    sly_score.setVisibility(View.GONE);
    gryff_score.setVisibility(View.GONE);
    haveball = false;
    listen_flag = false;
    mNote=NULL;
    
    // Handle all of our received NFC intents in this activity.
    mNfcPendingIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

    // Intent filters for reading a note from a tag or exchanging over p2p.
    IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
    try {
        ndefDetected.addDataType("text/plain");
    } catch (MalformedMimeTypeException e) { }
    mNdefExchangeFilters = new IntentFilter[] { ndefDetected };

    // Intent filters for writing to a tag
    IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
    mWriteTagFilters = new IntentFilter[] { tagDetected };
    
    
    //Send hello to connect to server
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      new fileSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"hello");
    } else {
      new fileSend().execute("hello");
    }
    

    wifiManager = (WifiManager) MainActivity.this.getSystemService(Context.WIFI_SERVICE); 
    rssiValue = wifiManager.getConnectionInfo().getRssi();
    System.out.println("RSSI Value: "+ String.valueOf(rssiValue));
    //Game starting - final game state set up
    findViewById(R.id.Goal).setVisibility(View.VISIBLE);
    findViewById(R.id.Slytherin).setVisibility(View.VISIBLE);
    findViewById(R.id.Gryffindor).setVisibility(View.VISIBLE);
    sly_score.setVisibility(View.VISIBLE);
    gryff_score.setVisibility(View.VISIBLE);
    findViewById(R.id.Bludger).setOnClickListener(ballCatcher);
    findViewById(R.id.Quaffle).setOnClickListener(ballCatcher);
    findViewById(R.id.Goal).setOnClickListener(goalTender);
 
  }

  @Override
  protected void onResume() {
    super.onResume();
    mResumed = true;
    System.out.println("In On Resume");
    
    // Sticky notes received from Android
    if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
        NdefMessage[] messages = getNdefMessages(getIntent());
        byte[] payload = messages[0].getRecords()[0].getPayload();
        setNoteBody(new String(payload));
        System.out.println("ONRESUME: NoteBody set as " + payload.toString() );
        setIntent(new Intent()); // Consume this intent.
    }
    enableNdefExchangeMode();
  }
  
  @Override
  protected void onPause() {
      super.onPause();
      System.out.println("In On Pause");
      mResumed = false;
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  protected void onNewIntent(Intent intent) {

    System.out.println("In On New Intent");
      // NDEF exchange mode
      if (!mWriteMode && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
          NdefMessage[] msgs = getNdefMessages(intent);
          System.out.println("Prompting for content");
          promptForContent(msgs[0]);
      }

      // Tag writing mode
      if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
          Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
          System.out.println("Writing to tag");
          writeTag(getNoteAsNdef(), detectedTag);
      }
  }  
  
  private View.OnClickListener mTagWriter = new View.OnClickListener() {
    
    @Override
    public void onClick(View arg0) {

      System.out.println("In On Click");
        mNote = (ImageView)arg0;
        // Write to a tag for as long as the dialog is shown.
        disableNdefExchangeMode();
        enableTagWriteMode();

        String action_msg="";
        String button_msg="";
        if(mNote.getContentDescription().toString().equalsIgnoreCase("Quaffle")){
          action_msg = "Score Goal";
          button_msg = "trygoal";
        }
        else if (mNote.getContentDescription().toString().equalsIgnoreCase("Bludger")){
          action_msg = "Knock out";
          button_msg = "tryknockout";
        }
        
        final String b_msg = button_msg;
        new AlertDialog.Builder(MainActivity.this).setTitle("Choose Action")
        .setPositiveButton(action_msg, new DialogInterface.OnClickListener() {
          
            @Override
            public final void onClick(DialogInterface arg0, int arg1) {
              mNote.setVisibility(View.GONE);
              haveball = false;
              mNote.setOnClickListener(ballCatcher);
              findViewById(R.id.Goal).setOnClickListener(goalTender);
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new fileSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,b_msg);
                } else {
                new fileSend().execute(b_msg);
                }
              disableTagWriteMode();
              enableNdefExchangeMode();
              mNote = NULL;
            }
        })
        
        .setNeutralButton("Transfer", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
              mNote.setVisibility(View.GONE); 
              haveball = false;
              mNote.setOnClickListener(ballCatcher);
              findViewById(R.id.Goal).setOnClickListener(goalTender);
              new AlertDialog.Builder(MainActivity.this).setTitle("Touch tag to write")
              .create().show();
              disableTagWriteMode();
              enableNdefExchangeMode();
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new fileSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"trans"+mNote.getContentDescription().toString());
                } else {
                new fileSend().execute("trans"+mNote.getContentDescription().toString());
                }
              mNote = NULL;
            }
        })
        
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
              mNote.setVisibility(View.VISIBLE);
              disableTagWriteMode();
              enableNdefExchangeMode();
              haveball = true;
            }
        }).show();
        
    }
    };
    
    private View.OnClickListener goalTender = new View.OnClickListener() {
      
      @Override
      public void onClick(View arg0) {

        System.out.println("In On Click of Goaltender");
        if(mNote==NULL){
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new fileSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"goalkeep");
        } else {
            new fileSend().execute("goalkeep");
        }
        }
        else if(MainActivity.this.cTimer!=null){
          toast("Already goalkeeping!");
        }
        else {
          toast("Drop ball to goalkeep!");
        }
 
      }
      };
    
    private View.OnClickListener ballCatcher = new View.OnClickListener() {
      
      @Override
      public void onClick(View arg0) {
        
        haveball = true;
        gk=false;
        ball.clearAnimation(); 
        mNote = (ImageView)arg0;
        if(cTimer!=null){
          cTimer.cancel();
          cTimer=null;
          gk=true;
        }
        mNote.setOnClickListener(mTagWriter);
        mNote.setVisibility(View.VISIBLE);
        new AlertDialog.Builder(MainActivity.this).setTitle("You caught the "+mNote.getContentDescription().toString()+"!")
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mNote.setVisibility(View.VISIBLE);
            }
        }).create().show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
          new fileSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"caught"+mNote.getContentDescription().toString());
        } else {
          new fileSend().execute("caught"+mNote.getContentDescription().toString());
        }
        if(gk){
          toast("You saved a goal!");
          gk=false;
        }
      }
      };
    
    private void promptForContent(final NdefMessage msg) {
      
      new AlertDialog.Builder(this).setTitle("Receive Ball/Revive?")
          .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface arg0, int arg1) {

                
                System.out.println("In On Click - Prompt for content");
                  String body = new String(msg.getRecords()[0].getPayload());
                  
                  if(body.equalsIgnoreCase("Revive")){
                    wifiManager = (WifiManager) MainActivity.this.getSystemService(Context.WIFI_SERVICE); 
                    wifiManager.setWifiEnabled(true);
                    String networkSSID = "SOUP BOY NETWORK";
                    String networkPass = "Thuvakudiboys";
                    WifiConfiguration conf = new WifiConfiguration();
                    conf.SSID = "\"" + networkSSID + "\""; 
                    conf.preSharedKey  = "\"" + networkPass + "\"";
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    netId = wifiManager.addNetwork(conf);
                    findViewById(R.id.Goal).setOnClickListener(goalTender);    
                    
                    //Put recontimer here
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                      new reconTimer().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                      new reconTimer().execute();
                    }
                    
                  }
                  else if(!body.equalsIgnoreCase("Nothing here")){
                    if(cTimer!=null){
                      cTimer.cancel();
                      cTimer=null;
                    }
                    System.out.println("Setting note body as "+body);
                    setNoteBody(body);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                      new fileSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"have"+mNote.getContentDescription().toString());
                    } else {
                      new fileSend().execute("have"+mNote.getContentDescription().toString());
                    }
                  }
                  
                  if(mNote == NULL){
                    System.out.println("mNote set as Null");
                  }
                  else {
                    findViewById(R.id.Goal).setOnClickListener(goalTender);
                    System.out.println("mNote set as " + mNote.getContentDescription().toString());
                    mNote.setOnClickListener(mTagWriter);
                  }
              }
          })
          .setNegativeButton("No", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface arg0, int arg1) {
               
              }
          }).show();
      }

  private void setNoteBody(String body) {
    System.out.println("In setNoteBody value: " + body);
    int resID = this.getResources().getIdentifier(body, "id", this.getPackageName());
    mNote = (ImageView) findViewById(resID);
    if(mNote!=NULL){
      mNote.setVisibility(View.VISIBLE);
    }
    
  }

  private NdefMessage getNoteAsNdef() {
    Charset charset = Charset.forName("UTF-8");
    System.out.println("In getNoteAsNdef");
    byte[] textBytes;
    if(mNote!= NULL){
      textBytes = mNote.getContentDescription().toString().getBytes(charset);
      System.out.println("TextBytes set:" + mNote.getContentDescription().toString());
    }
    else {
      textBytes = "Nothing here".getBytes();
      findViewById(R.id.Goal).setOnClickListener(goalTender);
      System.out.println("TextBytes set:" + "Nothing here");
    }
      
      NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain".getBytes(),
              new byte[] {}, textBytes);
      return new NdefMessage(new NdefRecord[] {
          textRecord
      });
  }
  
  
  NdefMessage[] getNdefMessages(Intent intent) {
    // Parse the intent
    NdefMessage[] msgs = null;
    System.out.println("In getNdefMessages");
    String action = intent.getAction();
    if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
            }
        } else {
            // Unknown tag type
            byte[] empty = new byte[] {};
            NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
            NdefMessage msg = new NdefMessage(new NdefRecord[] {
                record
            });
            msgs = new NdefMessage[] {
                msg
            };
        }
    } else {
        Log.d(TAG, "Unknown intent.");
        finish();
    }

    System.out.println("MSG returned: "+msgs[0].getRecords()[0].getPayload().toString());
    return msgs;
    }

  private void enableNdefExchangeMode() {
    mNfcAdapter.setNdefPushMessage(getNoteAsNdef(), MainActivity.this);
    mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mNdefExchangeFilters, null);
    
    if(mNote == NULL && listen_flag == false){
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        new fileReceive().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    } else {
        new fileReceive().execute();
    }
    }
    
    
    }

  private void disableNdefExchangeMode() {
    mNfcAdapter.disableForegroundDispatch(this);
    }

  private void enableTagWriteMode() {
    mWriteMode = true;
    IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
    mWriteTagFilters = new IntentFilter[] {
        tagDetected
    };
    mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
    }

  private void disableTagWriteMode() {
    mWriteMode = false;
    mNfcAdapter.disableForegroundDispatch(this);
    }
  
  boolean writeTag(NdefMessage message, Tag tag) {

    System.out.println("In WriteTag");
    int size = message.toByteArray().length;

    try {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            ndef.connect();

            if (!ndef.isWritable()) {
                toast("Tag is read-only.");
                return false;
            }
            if (ndef.getMaxSize() < size) {
                toast("Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size
                        + " bytes.");
                return false;
            }

            ndef.writeNdefMessage(message);
            toast("Wrote message to pre-formatted tag.");
            System.out.println("WRITETAG: Wrote ->" + message.toString());
            return true;
        } else {
            NdefFormatable format = NdefFormatable.get(tag);
            if (format != null) {
                try {
                    format.connect();
                    format.format(message);
                    toast("Formatted tag and wrote message");
                    return true;
                } catch (IOException e) {
                    toast("Failed to format tag.");
                    return false;
                }
            } else {
                toast("Tag doesn't support NDEF.");
                return false;
            }
        }
    } catch (Exception e) {
        toast("Failed to write tag");
    }

    return false;
    }

  private void toast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
  
  private class reconTimer extends AsyncTask<String, String, String>
  {

    @Override
    protected String doInBackground(String... arg0) {
      MainActivity.this.cm = (ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
      MainActivity.this.netInfo = cm.getActiveNetworkInfo();
      MainActivity.this.runOnUiThread(new Runnable() {
        public void run() {
          System.out.println("In File Send");
          MainActivity.this.tempTimer = new CountDownTimer(12000, 1000) {

            public void onTick(long millisUntilFinished) {
              final Toast t = Toast.makeText(MainActivity.this, "Waiting for WiFi " + millisUntilFinished / 1000 + " seconds", Toast.LENGTH_SHORT);
              t.show();
              handler = new Handler();
              handler.postDelayed(new Runnable() {
                 @Override
                 public void run() {
                     t.cancel(); 
                 }
          }, 999);
            }
            
            public void onFinish() {
              MainActivity.this.tempTimer = null;
              System.out.println("Sending the reconnect from here");
              if(MainActivity.this.netInfo != null && MainActivity.this.netInfo.getState() == NetworkInfo.State.CONNECTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                  new fileSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"reconnect");
                } else {
                  new fileSend().execute("reconnect");
                }
              }
            }
           }.start();
         
        }
      });
           
      return null;
    }
    
    
  }
  
  private class fileReceive extends AsyncTask<String, String, String>
  {
       int port = 4242;
       Boolean flag = true;
       ServerSocket serverSocket;
       Socket server;
       String values;
       String score;
       @Override
       protected String doInBackground(String... arg0) {

                  try{
                          serverSocket = new ServerSocket(port);
                          
                          while(flag){
                            MainActivity.this.listen_flag=true;
                            System.out.println("App listening...");
                            server = serverSocket.accept();
                            DataInputStream in = new DataInputStream(server.getInputStream());
                            values = in.readUTF().toString();
                            System.out.println("Received: "+values);
                            
                            if (values.equalsIgnoreCase("start-1")){
                              System.out.println("Comes to start1 - " + values);
                              MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                  Toast.makeText(MainActivity.this, "You joined Team Syltherin! Start Game!", Toast.LENGTH_SHORT).show();
                                }
                              });
                              
                            }
                            else if(values.equalsIgnoreCase("start-2")){
                              System.out.println("Comes to start2 - " + values);
                              MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                  Toast.makeText(MainActivity.this, "You joined Team Gryffindor! Start Game!", Toast.LENGTH_SHORT).show();
                                }
                              });
                            }
                            else if(values.contains(":")){
                              score = values;
                              System.out.println("Comes to contains - " + values);
                              System.out.println("Values in : is : "+values);
                              
                              MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                  Toast.makeText(MainActivity.this, "Incoming Score!", Toast.LENGTH_SHORT).show();
                                  TextView sly_score = (TextView) MainActivity.this.findViewById(R.id.Sly_score);
                                  TextView gryff_score = (TextView) MainActivity.this.findViewById(R.id.Gryff_score);
                                  sly_score.setText(score.substring(0, score.indexOf(":")));
                                  gryff_score.setText(score.substring(score.indexOf(":")+1, score.length()));
                                  sly_score.setVisibility(View.VISIBLE);
                                  gryff_score.setVisibility(View.VISIBLE);
                                }
                              });
                             
                            }
                            else if(values.equalsIgnoreCase("close")){
                              System.out.println("Comes to close- " + values);
                              flag = false;
                            }
                            else if(values.equalsIgnoreCase("knockout")){
                              if(MainActivity.this.cTimer!=null){
                                MainActivity.this.cTimer.cancel();
                                MainActivity.this.cTimer=null;
                              }
                              MainActivity.this.findViewById(R.id.Goal).setOnClickListener(goalTender);
                              if(mNote!=NULL){
                                mNote.setVisibility(View.GONE);
                                mNote=NULL;
                              }
                              System.out.println("Comes to knockout - " + values);
                              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                new fileSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"knockedout");
                              } else {
                                new fileSend().execute("knockedout");
                              }
                              
                            }
                            else if(values.equalsIgnoreCase("goal")){
                              System.out.println("Comes to goal - " + values);
                              MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                  Toast.makeText(MainActivity.this, "You scored a goal!", Toast.LENGTH_SHORT).show();
                                }
                              });
                            }
                            else if(values.equalsIgnoreCase("knockoutsuccess")){
                              MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                  Toast.makeText(MainActivity.this, "You knockedout the opponent!", Toast.LENGTH_SHORT).show();
                                }
                              });
                            }
                            else if(values.equalsIgnoreCase("missedgoal")){
                              MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                  Toast.makeText(MainActivity.this, "You missed the goal!", Toast.LENGTH_SHORT).show();
                                }
                              });
                            }
                            else {
                              gk=false;
                              if(cTimer!=null){
                                gk=true;
                              }
                              System.out.println("Comes to default - " + values);
                              MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                  MainActivity.ballReturn = values;
                                  if(MainActivity.ballReturn.equalsIgnoreCase("Bludger")){
                                    MainActivity.mNote = (ImageView) findViewById(R.id.Bludger); 
                                  }
                                  else if(MainActivity.ballReturn.equalsIgnoreCase("Quaffle")){
                                    MainActivity.mNote = (ImageView) findViewById(R.id.Quaffle);
                                  }
                                  MainActivity.mNote.setVisibility(View.VISIBLE); 
                                  ball = MainActivity.mNote;
                                  MainActivity.animation = new TranslateAnimation(-450.0f, 450.0f,
                                      0.0f, 0.0f);          
                                  MainActivity.animation.setDuration(700);   
                                  MainActivity.animation.setRepeatCount(5);  
                                  MainActivity.animation.setRepeatMode(2);   
                                  MainActivity.animation.setFillAfter(false);   
                                  MainActivity.ball.startAnimation(animation); 
                                  MainActivity.animation.setAnimationListener(new AnimationListener() {
                                    @Override
                                    public void onAnimationEnd(Animation arg0) {
                                        //Functionality here
                                      
                                      if(!MainActivity.this.haveball){
                                        MainActivity.ball.setVisibility(View.GONE);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                          new fileSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"missed"+values);
                                        } else {
                                          new fileSend().execute("missed"+values);
                                        }
                                        if(gk==true){
                                          Toast.makeText(MainActivity.this, "You didn't stop the goal!", Toast.LENGTH_SHORT).show();
                                        }
                                      }
                                      
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation arg0) {
                                      // TODO Auto-generated method stub
                                      
                                    }

                                    @Override
                                    public void onAnimationStart(Animation arg0) {
                                      // TODO Auto-generated method stub
                                      
                                    }
                                });
                                  
                                }
                                });
                            }
                            
                            System.out.println("Rx value:"+values);
                            server.close();
                          }
                          
                          MainActivity.this.listen_flag=false; 
                          serverSocket.close();    
                  }
                  catch (Exception e) {
                          e.printStackTrace();
                  }
            return values;
            }

          protected void onPostExecute(String values) {
                     // execution of result of Long time consuming operation
                     
                    }

           protected void onProgressUpdate(String... text) {
                    
                     // Things to be done while execution of long running operation is in
                     // progress. For example updating ProgessDialog
                    }
  }
  
  private class fileSend extends AsyncTask<String, String, String>
  {
    @Override
    protected String doInBackground(String... arg0) {
      System.out.println("Inside File Send do in bg");
      int port = 4343;
      Socket server;
      try {
        server = new Socket("192.168.0.34",port);
        DataOutputStream out = new DataOutputStream(server.getOutputStream());
        rssiValue = wifiManager.getConnectionInfo().getRssi();
        out.writeUTF(arg0[0]+";"+String.valueOf(rssiValue));
        System.out.println("RSSI Value: "+ String.valueOf(rssiValue));
        server.close();
      } catch (UnknownHostException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      if(arg0[0].equalsIgnoreCase("goalkeep")){
        MainActivity.this.findViewById(R.id.Quaffle).setOnClickListener(ballCatcher);
        MainActivity.this.findViewById(R.id.Bludger).setOnClickListener(ballCatcher);
        MainActivity.this.runOnUiThread(new Runnable() {
          public void run() {
            System.out.println("In File Send");
            MainActivity.this.cTimer = new CountDownTimer(15000, 1000) {

              public void onTick(long millisUntilFinished) {
                final Toast t = Toast.makeText(MainActivity.this, "Goal keeping for next " + millisUntilFinished / 1000 + " seconds", Toast.LENGTH_SHORT);
                t.show();
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                   @Override
                   public void run() {
                       t.cancel(); 
                   }
            }, 999);
              }
              
              public void onFinish() {
                MainActivity.this.findViewById(R.id.Goal).setOnClickListener(goalTender);
                MainActivity.this.cTimer = null;
              }
             }.start();
            
             
          }
        });
      }
      else if(arg0[0].equalsIgnoreCase("knockedout")){
        wifiManager = (WifiManager) MainActivity.this.getSystemService(Context.WIFI_SERVICE); 
        wifiManager.setWifiEnabled(false);
        wifiManager.removeNetwork(netId);
        wifiManager.saveConfiguration();
      }
      

      return "Sent";
      
    }
  }

}
