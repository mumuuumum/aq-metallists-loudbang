package aq.metallists.loudbang;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import aq.metallists.loudbang.cutil.CJarInterface;

public class AboutWindow extends AppCompatActivity {
    SharedPreferences sp = null;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_window);
        Toolbar toolbar = findViewById(R.id.about_toolbar);
        setSupportActionBar(toolbar);

        this.sp = PreferenceManager.getDefaultSharedPreferences(this);

        EditText about_snck_edit = findViewById(R.id.about_snck_edit);
        Button about_snck_btn = findViewById(R.id.about_snck_btn);
        about_snck_edit.setText(Short.toString((short) sp.getInt("sndgen.volume", CJarInterface.DEFAULT_VOLUME)));
        about_snck_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                short tgtVol = (short) sp.getInt("sndgen.volume", CJarInterface.DEFAULT_VOLUME);

                try {
                    int ckVal = Integer.parseInt(String.valueOf(about_snck_edit.getText()));
                    if (ckVal > 32767) {
                        Toast.makeText(AboutWindow.this,R.string.str_btn_soundcheck_toobig,Toast.LENGTH_LONG).show();

                        return;
                    }
                    if (ckVal < -32767) {
                        Toast.makeText(AboutWindow.this,R.string.str_btn_soundcheck_toolow,Toast.LENGTH_LONG).show();
                        return;
                    }
                    tgtVol = Short.parseShort(String.valueOf(about_snck_edit.getText()));
                } catch (Exception x) {
                    Toast.makeText(AboutWindow.this,R.string.str_btn_soundcheck_nan,Toast.LENGTH_LONG).show();
                    return;
                }

                byte[] sound = CJarInterface.GenTestBeep(tgtVol, 0);

                int output_line = AudioManager.STREAM_VOICE_CALL;

                switch (sp.getString("tx_output", "music")) {
                    case "ring":
                        output_line = AudioManager.STREAM_RING;
                        break;
                    case "music":
                        output_line = AudioManager.STREAM_MUSIC;
                        break;
                    case "alarm":
                        output_line = AudioManager.STREAM_ALARM;
                        break;
                    //case "call":
                    default:
                        output_line = AudioManager.STREAM_VOICE_CALL;
                        break;
                }

                AudioTrack track = new AudioTrack(output_line,
                        12000, //sample rate
                        AudioFormat.CHANNEL_OUT_MONO, //2 channel
                        AudioFormat.ENCODING_PCM_16BIT, // 16-bit
                        sound.length,
                        AudioTrack.MODE_STATIC);

                track.write(sound, 0, sound.length);
                track.play();

            }
        });
    }
}
