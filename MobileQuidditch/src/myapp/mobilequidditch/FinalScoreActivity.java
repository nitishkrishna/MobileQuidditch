package myapp.mobilequidditch;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class FinalScoreActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_final_score);
    TextView ss = (TextView)findViewById(R.id.Sly_score);
    ss.setText(MainActivity.score1.toString());
    TextView gs = (TextView)findViewById(R.id.Gryff_score);
    gs.setText(MainActivity.score2.toString());
    findViewById(R.id.Sly_score).setVisibility(View.VISIBLE);
    findViewById(R.id.Gryff_score).setVisibility(View.VISIBLE);
    
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.final_score, menu);
    return true;
  }

}
