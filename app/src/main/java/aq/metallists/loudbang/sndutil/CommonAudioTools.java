package aq.metallists.loudbang.sndutil;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class CommonAudioTools {
    public static final int SAMPLE_RATE = 12000; // Hz
    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    public static final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
}
