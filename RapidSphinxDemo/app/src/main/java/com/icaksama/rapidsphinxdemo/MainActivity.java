package com.icaksama.rapidsphinxdemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.icaksama.rapidsphinx.RapidCompletionListener;
import com.icaksama.rapidsphinx.RapidPreparationListener;
import com.icaksama.rapidsphinx.RapidSphinx;
import com.icaksama.rapidsphinx.RapidSphinxListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.pocketsphinx.Config;

public class MainActivity extends AppCompatActivity implements RapidSphinxListener {

    private RapidSphinx rapidSphinx;
    private Button btnRecognizer;
    private Button btnStartAudio;
    private Button btnSync;
    private EditText txtWords;
    private EditText txtDistractor;
    private TextView txtResult;
    private TextView txtStatus;
    private TextView txtPartialResult;
    private TextView txtUnsupported;

    private ProgressDialog dialog = null;

    private List<String> finalHyp = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rapidSphinx = new RapidSphinx(this);
        rapidSphinx.addListener(this);
        if (isPermissionsGranted()) {
            dialog = ProgressDialog.show(MainActivity.this, "",
                    "Preparing data. Please wait...", true);
            rapidSphinx.prepareRapidSphinx(new RapidPreparationListener() {
                @Override
                public void rapidPreExecute(Config config) {
//                    rapidSphinx.setRawLogAvailable(true);
//                    config.setString("-logfn", "/dev/null");
//                    config.setBoolean("-verbose", true);
                    rapidSphinx.setStopAtEndOfSpeech(true);
                    rapidSphinx.setSilentToDetect(2.0f);
                }

                @Override
                public void rapidPostExecute(boolean isSuccess) {
                    btnSync.setEnabled(true);
                    btnRecognizer.setEnabled(false);
                    txtStatus.setText("RapidSphinx ready!");
                    dialog.dismiss();
                }
            });
        }

//        rapidSphinx.prepareRapidSphinxFullLM(new RapidPreparationListener() {
//            @Override
//            public void rapidPreExecute(Config config) {
//                rapidSphinx.setRawLogAvailable(true);
//                config.setString("-logfn", "/dev/null");
//                config.setBoolean("-verbose", true);
//            }
//
//            @Override
//            public void rapidPostExecute(boolean b) {
////                btnSync.setEnabled(true);
////                btnRecognizer.setEnabled(false);
//                txtStatus.setText("RapidSphinx ready!");
//                dialog.dismiss();
//            }
//        });

        txtWords = (EditText) findViewById(R.id.txtWords);
        txtDistractor = (EditText) findViewById(R.id.txtDistractor);
        txtResult = (TextView) findViewById(R.id.txtResult);
        txtPartialResult = (TextView) findViewById(R.id.txtPartialResult);
        txtUnsupported = (TextView) findViewById(R.id.txtUnsuported);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        btnSync = (Button) findViewById(R.id.btnSync);
        btnRecognizer = (Button) findViewById(R.id.btnRecognizer);
        btnStartAudio = (Button) findViewById(R.id.btnStartAudio);
        txtStatus.setText("Preparing data!");

        // Disable buttons for the first time
        btnSync.setEnabled(false);
        btnRecognizer.setEnabled(false);

        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
                btnSync.setEnabled(false);
                btnRecognizer.setEnabled(false);
                rapidSphinx.updateVocabulary(txtWords.getText().toString().trim(),
                        txtDistractor.getText().toString().trim().split(" "), new RapidCompletionListener() {
                    @Override
                    public void rapidCompletedProcess() {
                        txtResult.setText("");
                        txtPartialResult.setText("");
                        txtStatus.setText("");
                        txtUnsupported.setText("");
                        btnRecognizer.setEnabled(true);
                        dialog.dismiss();
                    }
                });
            }
        });

        btnRecognizer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtResult.setText("");
                txtPartialResult.setText("");
                txtStatus.setText("");
                txtUnsupported.setText("");
                btnSync.setEnabled(false);
                btnRecognizer.setEnabled(false);
                rapidSphinx.startRapidSphinx(10);
                txtStatus.setText("Speech NOW!");
            }
        });

        btnStartAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (rapidSphinx.getRapidRecorder() != null) {
                        rapidSphinx.getRapidRecorder().play(new RapidCompletionListener() {
                            @Override
                            public void rapidCompletedProcess() {
                                System.out.println("Audio finish!");
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean isPermissionsGranted() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        dialog = ProgressDialog.show(MainActivity.this, "",
                "Preparing data. Please wait...", true);
        rapidSphinx.prepareRapidSphinx(new RapidPreparationListener() {
            @Override
            public void rapidPreExecute(Config config) {
                rapidSphinx.setRawLogAvailable(true);
                config.setString("-logfn", "/dev/null");
                config.setBoolean("-verbose", true);
            }

            @Override
            public void rapidPostExecute(boolean isSuccess) {
                btnSync.setEnabled(true);
                btnRecognizer.setEnabled(false);
                txtStatus.setText("RapidSphinx ready!");
                dialog.dismiss();
            }
        });
    }

    @Override
    public void rapidSphinxDidStop(String reason, int code) {
        btnSync.setEnabled(true);
        btnRecognizer.setEnabled(true);
        System.out.println();
        if (code == 500) { // 200 code for error
            System.out.println(reason);
        } else if (code == 522) { // 200 code for timed out
            System.out.println(reason);
        } else if (code == 200) { // 200 code for finish speech
            System.out.println(reason);
        }
    }

    @Override
    public void rapidSphinxFinalResult(String result, List<String> hypArr, List<Double> scores) {
        if (result.equalsIgnoreCase(txtWords.getText().toString())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                txtResult.setTextColor(getResources().getColor(android.R.color.holo_green_light, null));
            } else {
                txtResult.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                txtResult.setTextColor(getResources().getColor(android.R.color.holo_red_light, null));
            } else {
                txtResult.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
        }
        txtResult.setText(result);
    }

    @Override
    public void rapidSphinxPartialResult(String partialResult) {
        txtPartialResult.setText(partialResult);
    }

    @Override
    public void rapidSphinxUnsupportedWords(List<String> words) {
        String unsupportedWords = "";
        for (String word: words) {
            unsupportedWords += word + ", ";
        }
        txtUnsupported.setText("Unsupported words : \n" + unsupportedWords);
    }

    @Override
    public void rapidSphinxDidSpeechDetected() {
        txtStatus.setText("Speech detected!");
    }

    @Override
    public  void rapidSphinxBuffer(short[] shortBuffer, byte[] byteBuffer,boolean inSpeech) {
        System.out.println(shortBuffer.length + " - " + byteBuffer.length + " - " + inSpeech);
    }
}
