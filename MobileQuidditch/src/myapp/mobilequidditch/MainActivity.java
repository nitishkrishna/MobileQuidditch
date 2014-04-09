package myapp.mobilequidditch;


import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity{

  private static final String TAG = "NitishKrishna";
  private static final ImageButton NULL = null;
  private boolean mResumed = false;
  private boolean mWriteMode = false;
  NfcAdapter mNfcAdapter;
  static ImageView mNote;
  static ImageView ball;
  static Animation animation;
  static String ballReturn;
  private static Context mContext;
  PendingIntent mNfcPendingIntent;
  IntentFilter[] mWriteTagFilters;
  IntentFilter[] mNdefExchangeFilters;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mContext = MainActivity.this.getApplicationContext();
    System.out.println("In On Create");
    mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    findViewById(R.id.Bludger).setOnClickListener(ballCatcher);
    findViewById(R.id.Quaffle).setOnClickListener(ballCatcher);
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
        
        new AlertDialog.Builder(MainActivity.this).setTitle("Initiate transfer?")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
              mNote.setVisibility(View.GONE); 
              mNote.setOnClickListener(ballCatcher);
              mNote = NULL;
              new AlertDialog.Builder(MainActivity.this).setTitle("Touch tag to write")
              .create().show();
              disableTagWriteMode();
              enableNdefExchangeMode();
            }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
              mNote.setVisibility(View.VISIBLE);
              disableTagWriteMode();
              enableNdefExchangeMode();
            }
        }).show();
        
    }
    };
    
    private View.OnClickListener ballCatcher = new View.OnClickListener() {
      
      @Override
      public void onClick(View arg0) {
        
        ball.clearAnimation(); 
        mNote = (ImageView)arg0;
        mNote.setOnClickListener(mTagWriter);
        mNote.setVisibility(View.VISIBLE);
        new AlertDialog.Builder(MainActivity.this).setTitle("You caught the "+mNote.getContentDescription().toString()+"!")
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mNote.setVisibility(View.VISIBLE);
            }
        }).create().show();
      }
      };
    
    private void promptForContent(final NdefMessage msg) {
      
      new AlertDialog.Builder(this).setTitle("Replace current content?")
          .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface arg0, int arg1) {

                
                System.out.println("In On Click - Prompt for content");
                  String body = new String(msg.getRecords()[0].getPayload());
                  
                  if(body.equalsIgnoreCase("Revive")){
                    WifiManager wifiManager = (WifiManager) MainActivity.this.getSystemService(Context.WIFI_SERVICE); 
                    wifiManager.setWifiEnabled(true);
                  }
                  else {
                    setNoteBody(body);
                  }
                  
                  if(mNote == NULL){
                    System.out.println("mNote set as Null");
                  }
                  else {
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
    mNote.setVisibility(View.VISIBLE);
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
    
    if(mNote == NULL){
      new fileReceive().execute();
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
  
  
  private class fileReceive extends AsyncTask<String, String, String>
  {
       int port = 4242;
       String values;

       @Override
       protected String doInBackground(String... arg0) {

                  try{
                          ServerSocket serverSocket = new ServerSocket(port);
                          System.out.println("App listening...");
                          Socket server = serverSocket.accept();
                          DataInputStream in = new DataInputStream(server.getInputStream());
                          values = in.readUTF().toString();
                          MainActivity.this.runOnUiThread(new Runnable() {
                          public void run() {
                            Toast.makeText(MainActivity.this, values, Toast.LENGTH_SHORT).show();
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
                                MainActivity.ball.setVisibility(View.GONE);
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
                          System.out.println("Rx value:"+values);
                          server.close();
                          //flag = false;
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
  
  
}
