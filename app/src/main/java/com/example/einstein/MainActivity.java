package com.example.einstein;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private boolean isPlaying = false;
    private AudioTrack audioTrack;
    private Thread toneThread;

    private double frequency = 440.0;
    private final int sampleRate = 44100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button toggleButton = findViewById(R.id.toggleButton);
        SeekBar freqSeek = findViewById(R.id.freqSeek);
        TextView freqText = findViewById(R.id.freqText);

        freqSeek.setProgress(420);

        freqSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                frequency = progress + 20;
                freqText.setText("Frequency: " + (int) frequency + " Hz");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        toggleButton.setOnClickListener(v -> {

            if (!isPlaying) {
                isPlaying = true;
                startTone();
                toggleButton.setText("Stop Tone");
            } else {
                isPlaying = false;
                stopTone();
                toggleButton.setText("Start Tone");
            }

        });
    }

    private void startTone() {

        int bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
        );

        audioTrack.play();

        toneThread = new Thread(() -> {

            short[] buffer = new short[bufferSize];
            double phase = 0;

            while (isPlaying) {

                for (int i = 0; i < buffer.length; i++) {

                    buffer[i] = (short)(Math.sin(phase) * 32767);

                    phase += 2 * Math.PI * frequency / sampleRate;

                    if (phase > 2 * Math.PI)
                        phase -= 2 * Math.PI;

                }

                audioTrack.write(buffer, 0, buffer.length);
            }

        });

        toneThread.start();
    }

    private void stopTone() {

        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }

    }
}