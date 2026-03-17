package com.example.einstein;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private AudioTrack audioTrack;
    private Thread audioThread;

    private volatile boolean isPlaying = false;
    private volatile boolean sweepMode = false;

    private double frequency = 440;
    private double volume = 0.5;

    // Sweep indstillinger
    private double sweepStart = 20;
    private double sweepEnd = 20000;
    private double sweepStepPerBuffer = 1;

    private final int sampleRate = 44100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button toggle = findViewById(R.id.toggleButton);
        Button sweepBtn = findViewById(R.id.sweepButton);

        SeekBar freqSeek = findViewById(R.id.freqSeek);
        SeekBar volumeSeek = findViewById(R.id.volumeSeek);

        TextView freqText = findViewById(R.id.freqText);
        EditText manualFreqInput = findViewById(R.id.manualFreqInput);
        
        EditText sweepStartInput = findViewById(R.id.sweepStartInput);
        EditText sweepEndInput = findViewById(R.id.sweepEndInput);
        EditText sweepSpeedInput = findViewById(R.id.sweepSpeedInput);

        freqSeek.setMax(19980);
        freqSeek.setProgress(420);

        // Lytter på manuel indtastning
        manualFreqInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!sweepMode && manualFreqInput.hasFocus()) {
                    try {
                        double f = Double.parseDouble(s.toString());
                        if (f >= 1 && f <= 22000) {
                            frequency = f;
                            freqText.setText((int)frequency + " Hz");
                            freqSeek.setProgress((int)frequency - 20);
                        }
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        freqSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !sweepMode) {
                    frequency = progress + 20;
                    freqText.setText((int)frequency + " Hz");
                    manualFreqInput.setText(String.valueOf((int)frequency));
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar){}
            public void onStopTrackingTouch(SeekBar seekBar){}
        });

        volumeSeek.setMax(100);
        volumeSeek.setProgress(50);
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
                toggle.setText("Stop Lyd");
            }else{
                isPlaying = false;
                stopTone();
                toggle.setText("Start Lyd");
            }
        });

        sweepBtn.setOnClickListener(v -> {
            try {
                sweepStart = Double.parseDouble(sweepStartInput.getText().toString());
                sweepEnd = Double.parseDouble(sweepEndInput.getText().toString());
                double speedHzPerSec = Double.parseDouble(sweepSpeedInput.getText().toString());
                
                sweepStepPerBuffer = speedHzPerSec * 0.05; 

                sweepMode = !sweepMode;
                if (sweepMode) {
                    sweepBtn.setText("Stop Sweep");
                    frequency = sweepStart;
                    manualFreqInput.setEnabled(false); // Deaktiver manuel input under sweep
                } else {
                    sweepBtn.setText("Aktiver Sweep");
                    manualFreqInput.setEnabled(true);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Tjek dine talværdier", Toast.LENGTH_SHORT).show();
            }
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
            TextView freqDisplay = findViewById(R.id.freqText);

            while(isPlaying){
                if(sweepMode){
                    frequency += (sweepStepPerBuffer / (sampleRate / (double)bufferSize));
                    if(frequency > sweepEnd) frequency = sweepStart;
                    
                    final int currentFreq = (int)frequency;
                    runOnUiThread(() -> freqDisplay.setText(currentFreq + " Hz"));
                }

                for(int i=0; i<buffer.length; i++){
                    double sample = Math.sin(phase) * volume;
                    buffer[i] = (short)(sample * 32767);
                    phase += 2 * Math.PI * frequency / sampleRate;
                    if(phase > 2 * Math.PI) phase -= 2 * Math.PI;
                }
                audioTrack.write(buffer, 0, buffer.length);
            }
        });
        audioThread.start();
    }

    private void stopTone(){
        isPlaying = false;
        if(audioThread != null) {
            try { audioThread.join(); } catch (InterruptedException e) {}
            audioThread = null;
        }
        if(audioTrack != null){
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
}