package aq.metallists.loudbang;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.util.Locale;

import aq.metallists.loudbang.cutil.CJarInterface;
import aq.metallists.loudbang.sndutil.AudioRecordTools;

public class AboutWindow extends AppCompatActivity {
    SharedPreferences sp = null;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_window);
        Toolbar toolbar = findViewById(R.id.about_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.cardview_light_background));

        this.sp = PreferenceManager.getDefaultSharedPreferences(this);

        TextView about_longtext = findViewById(R.id.about_longtext);

        try {
            String info = this.getAudioInformation();
            about_longtext.setText(
                    about_longtext.getText() + "\n\n" +
                            getString(R.string.sndinput_success_acquiring)
                            + "\n"
                            + info
            );
        } catch (Exception x) {
            about_longtext.setText(
                    about_longtext.getText()
                            + "\n\n"
                            + getString(R.string.sndinput_error_acquiring)
                            + "\n"
                            + x.getMessage()
            );
        }

        EditText about_snck_edit = findViewById(R.id.about_snck_edit);
        Button about_snck_btn = findViewById(R.id.about_snck_btn);
        about_snck_edit.setText(Short.toString((short) sp.getInt("tx_soundampl", CJarInterface.DEFAULT_VOLUME)));
        about_snck_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                short tgtVol = (short) sp.getInt("sndgen.volume", CJarInterface.DEFAULT_VOLUME);

                try {
                    int ckVal = Integer.parseInt(String.valueOf(about_snck_edit.getText()));
                    if (ckVal > 32767) {
                        Toast.makeText(AboutWindow.this, R.string.str_btn_soundcheck_toobig, Toast.LENGTH_LONG).show();

                        return;
                    }
                    if (ckVal < -32767) {
                        Toast.makeText(AboutWindow.this, R.string.str_btn_soundcheck_toolow, Toast.LENGTH_LONG).show();
                        return;
                    }
                    tgtVol = Short.parseShort(String.valueOf(about_snck_edit.getText()));
                } catch (Exception x) {
                    Toast.makeText(AboutWindow.this, R.string.str_btn_soundcheck_nan, Toast.LENGTH_LONG).show();
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

    private String getAudioInformation() throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return getApplicationContext().getString(R.string.sndinput_low_android_version);
        }
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] inputDevices = am.getDevices(AudioManager.GET_DEVICES_INPUTS);
        StringBuilder report = new StringBuilder();
        for (AudioDeviceInfo dev : inputDevices) {
            report.append(dev.getId()).append(" -> ");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                report.append(
                        String.format(Locale.GERMAN, "\"%s\" ", dev.getAddress())
                );
            }
            report
                    .append(dev.getProductName())
                    .append(" ")
                    .append(AudioRecordTools.getAudioInputNameByType(dev.getType()))
                    .append("\n");
        }

        return report.toString();
    }


}
