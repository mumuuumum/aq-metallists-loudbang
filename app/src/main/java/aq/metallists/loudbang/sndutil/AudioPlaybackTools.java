package aq.metallists.loudbang.sndutil;

import static aq.metallists.loudbang.sndutil.CommonAudioTools.ENCODING;
import static aq.metallists.loudbang.sndutil.CommonAudioTools.SAMPLE_RATE;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import aq.metallists.loudbang.cutil.CJarInterface;

public class AudioPlaybackTools {
    public static short getVolume(SharedPreferences sp) {
        try {
            return (short) sp.getInt("tx_soundampl", CJarInterface.DEFAULT_VOLUME);
        } catch (Exception x) {
            Log.e("ERROR", "Exception at getVolume:", x);
        }
        return CJarInterface.DEFAULT_VOLUME;
    }


    public static int getOffset() {
        return (int) ((Math.random() * (70 - (-75) + 1)) + (-75));
    }

    public static AudioTrack createAudioPlayer(SharedPreferences sp, int length) {
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
            default:
                break;
        }

        AudioTrack audio = new AudioTrack(output_line,
                SAMPLE_RATE, //sample rate
                AudioFormat.CHANNEL_OUT_MONO, //2 channel
                ENCODING, // 16-bit
                length,
                AudioTrack.MODE_STATIC);



        return audio;
    }
}
