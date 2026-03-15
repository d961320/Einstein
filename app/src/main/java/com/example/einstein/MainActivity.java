package com.example.einstein;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private AudioTrack audioTrack;
    private Thread audioThread;

    private boolean isPlaying = false;
    private boolean sweepMode = false;

    private double frequency = 440;
    private double volume = 0.5;

    private final int sampleRate = 44100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button toggle = findViewById(R.id.toggleButton);
        Button sweep = findViewById(R.id.sweepButton);

        SeekBar freqSeek = findViewById(R.id.freqSeek);
        SeekBar volumeSeek = findViewById(R.id.volumeSeek);

        TextView freqText = findViewById(R.id.freqText);
        EditText freqInput = findViewById(R.id.freqInput);

        freqSeek.setMax(19980);

        freqSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                frequency = progress + 20;
                freqText.setText((int)frequency + " Hz");

            }

            public void onStartTrackingTouch(SeekBar seekBar){}
            public void onStopTrackingTouch(SeekBar seekBar){}
        });

        volumeSeek.setMax(100);

        volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volume = progress / 100.0;
            }

            public void onStartTrackingTouch(SeekBar seekBar){}
            public void onStopTrackingTouch(SeekBar seekBar){}
        });

        toggle.setOnClickListener(v -> {

            if(!isPlaying){
                isPlaying = true;
                startTone();
                toggle.setText("Stop");
            }else{
                isPlaying = false;
                stopTone();
                toggle.setText("Start");
            }

        });

        sweep.setOnClickListener(v -> {
            sweepMode = !sweepMode;
        });

        freqInput.setOnEditorActionListener((v, actionId, event) -> {

            try{
                frequency = Double.parseDouble(freqInput.getText().toString());
                freqText.setText((int)frequency + " Hz");
            }catch(Exception ignored){}

            return false;
        });

    }

    private void startTone(){

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

        audioThread = new Thread(() -> {

            short[] buffer = new short[bufferSize];
            double phase = 0;

            while(isPlaying){

                if(sweepMode){
                    frequency += 1;
                    if(frequency > 20000) frequency = 20;
                }

                for(int i=0;i<buffer.length;i++){

                    double sample = Math.sin(phase) * volume;

                    buffer[i] = (short)(sample * 32767);

                    phase += 2*Math.PI*frequency/sampleRate;

                    if(phase > 2*Math.PI)
                        phase -= 2*Math.PI;

                }

                audioTrack.write(buffer,0,buffer.length);

            }

        });

        audioThread.start();
    }

    private void stopTone(){

        if(audioTrack != null){

            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;

        }

    }
}