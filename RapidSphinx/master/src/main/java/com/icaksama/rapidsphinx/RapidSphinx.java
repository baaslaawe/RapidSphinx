package com.icaksama.rapidsphinx;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.NGramModel;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.Segment;
import edu.cmu.pocketsphinx.SegmentIterator;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

/**
 * Created by icaksama on 16/11/17.
 */

public class RapidSphinx implements RecognitionListener {

    // Basic Setting
    private String SEARCH_ID = "icaksama";
    private RapidRecognizer rapidRecognizer;
    private RapidRecognizer rapidRecognizerTemp;
    private Context context;
//    private String[] words = null;
    private Utilities util = new Utilities();
    private RapidSphinxListener rapidSphinxListener = null;
    private File assetDir = null;

    // Additional Setting
    private boolean rawLogAvailable = false;
    private long sampleRate = 16000;
    private float vadThreshold = (float) 3.0;

    private List<String> originalWords = new ArrayList<String>();
    private List<String> oovWords = new ArrayList<String>();
    private List<String> vocabularies = new ArrayList<String>();
    private List<String> unsupportedWords = new ArrayList<String>();
    private List<String> hypArr = new ArrayList<String>();
    private List<Double> scores = new ArrayList<Double>();
    private Config config = null;
    private NGramModel nGramModel = null;

    public RapidSphinx(Context context) {
        this.context = context;
        try {
            Assets assetsContext = new Assets(context);
            assetDir = assetsContext.syncAssets();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateDictonary(String[] words) {
        unsupportedWords.clear();
        try {
            File fileOut = new File(assetDir, "arpas/"+ SEARCH_ID +".dict");
            if (fileOut.exists()){
                fileOut.delete();
            }
            FileOutputStream outputStream = new FileOutputStream(fileOut);
            for (String word: words) {
                String pronoun = rapidRecognizerTemp.getRapidDecoder().lookupWord(word.trim());
                if (pronoun != null) {
                    String wordN = word + " ";
                    outputStream.write(wordN.toLowerCase(Locale.ENGLISH).getBytes(Charset.forName("UTF-8")));
                    outputStream.write(pronoun.getBytes(Charset.forName("UTF-8")));
                    outputStream.write(System.getProperty("line.separator").getBytes());
                } else {
                    unsupportedWords.add(word.trim());
                }
            }
            outputStream.close();
            rapidRecognizer.getRapidDecoder().loadDict(fileOut.getPath(), null, "dict");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateNGramModel(final String[] words) {
        if (nGramModel != null) {
            rapidRecognizer.getRapidDecoder().getLm(SEARCH_ID).delete();
            rapidRecognizer.getRapidDecoder().unsetSearch(SEARCH_ID);
        }
        File oldFile = new File(assetDir, SEARCH_ID + ".arpa");
        File newFile = new File(assetDir, "arpas/"+ SEARCH_ID +"-new.arpa");
        try {
            if (newFile.exists()) {
                newFile.delete();
            }
            util.copyFile(oldFile, newFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        nGramModel = new NGramModel(rapidRecognizer.getRapidDecoder().getConfig(),
                rapidRecognizer.getRapidDecoder().getLogmath(), newFile.getPath());
        for (String word: words) {
            nGramModel.addWord(word.trim().toLowerCase(Locale.ENGLISH), 1.7f);
        }
        rapidRecognizer.getRapidDecoder().setLm(SEARCH_ID, nGramModel);
    }

    /**
     * Add new single word in current dictionary file. Make sure to remove the duplicate.
     * Make sure to remove the punctuation and change the number to string.
     * @param word new word want to add in the dictionary
     * @param pronunciation the pronunciation of the new word
     * @param rapidCompletionListener Give feedback when word already added
     */
    public void addDictWord(final String word, final String pronunciation, @Nullable final RapidCompletionListener rapidCompletionListener) {
        String vocab = rapidRecognizerTemp.getRapidDecoder().lookupWord(word.trim().toLowerCase(Locale.ENGLISH));
        if (vocab == null) {
            new AsyncTask<Void, Void, Exception>(){
                @Override
                protected void onPreExecute() {}
                @Override
                protected Exception doInBackground(Void... params) {
                    rapidRecognizer.getRapidDecoder().addWord(word.trim().toLowerCase(Locale.ENGLISH),
                            pronunciation.trim().toUpperCase(Locale.ENGLISH), 1);
                    return null;
                }
                @Override
                protected void onPostExecute(Exception e) {
                    if (rapidCompletionListener != null) {
                        rapidCompletionListener.rapidCompletedProcess();
                    }
                }
            }.execute();
        } else {
            System.out.println(word + " " + vocab + " already exist in dictionary.");
        }
    }

    /**
     * Add new words in one time to the dictionary file. Make sure to remove the duplicate.
     * Make sure to remove the punctuation and change the number to string.
     * @param words new words in array string in lowercase
     * @param pronunciation the pronunciation of new words in array string
     * @param rapidCompletionListener Give feedback when word already added
     */
    public void addDictWords(final String[] words, final String[] pronunciation, @Nullable final RapidCompletionListener rapidCompletionListener) {
        if (words.length == pronunciation.length) {
            for (int i = 0; i < words.length; i++) {
                String vocab = rapidRecognizerTemp.getRapidDecoder().lookupWord(words[i].trim().toLowerCase(Locale.ENGLISH));
                if (vocab == null) {
                    final int finalI = i;
                    new AsyncTask<Void, Void, Exception>(){
                        @Override
                        protected void onPreExecute() {}
                        @Override
                        protected Exception doInBackground(Void... params) {
                            rapidRecognizer.getRapidDecoder().addWord(words[finalI].trim().toLowerCase(Locale.ENGLISH),
                                    pronunciation[finalI].trim().toUpperCase(Locale.ENGLISH), 1);
                            return null;
                        }
                        @Override
                        protected void onPostExecute(Exception e) {
                            if (rapidCompletionListener != null) {
                                rapidCompletionListener.rapidCompletedProcess();
                            }
                        }
                    }.execute();
                } else {
                    System.out.println(words[i] + " " + vocab + " already exist in dictionary.");
                }
            }
            if (rapidCompletionListener != null) {
                rapidCompletionListener.rapidCompletedProcess();
            }
        } else {
            System.out.println("Total words and pronunciation are difference.");
        }
    }

    /**
     * Add new words to current language model. Make sure the language model already set before call this function.
     * Make sure to remove the punctuation and change the number to string.
     * @param words new words in array string.
     * @param rapidCompletionListener Give feedback when word already added
     */
    public void addLMWords(final String[] words, @Nullable final RapidCompletionListener rapidCompletionListener) {
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected void onPreExecute() {}
            @Override
            protected Exception doInBackground(Void... params) {
                NGramModel lmModel = rapidRecognizer.getRapidDecoder().getLm(SEARCH_ID);
                System.out.println(lmModel);
                for (String word: words) {
                    int a = lmModel.addWord(word.trim().toLowerCase(Locale.ENGLISH), 1.7f);
                    System.out.println("Result word: " + a);
                    if (a < 0) {
                        System.out.println(word + " doesn't support. Please add word \""+ word + "\" in dictionary.");
                    }
                }
                rapidRecognizer.getRapidDecoder().setLm(SEARCH_ID, lmModel);
                return null;
            }
            @Override
            protected void onPostExecute(Exception e) {
                if (rapidCompletionListener != null) {
                    rapidCompletionListener.rapidCompletedProcess();
                }
            }
        }.execute();
    }

    /**
     * Add new single word to current language model. Make sure the language model already set before call this function.
     * Make sure to remove the punctuation and change the number to string.
     * @param word new single word
     * @param rapidCompletionListener Give feedback when word already added
     */
    public void addLMWord(final String word, @Nullable final RapidCompletionListener rapidCompletionListener) {
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected void onPreExecute() {}
            @Override
            protected Exception doInBackground(Void... params) {
                NGramModel lmModel = rapidRecognizer.getRapidDecoder().getLm(SEARCH_ID);
                if (lmModel.addWord(word.trim().toLowerCase(Locale.ENGLISH), 1.7f) < 0) {
                    System.out.println(word + " doesn't support. Please add word \""+ word + "\" in dictionary.");
                }
                rapidRecognizer.getRapidDecoder().setLm(SEARCH_ID, lmModel);
                return null;
            }
            @Override
            protected void onPostExecute(Exception e) {
                if (rapidCompletionListener != null) {
                    rapidCompletionListener.rapidCompletedProcess();
                }
            }
        }.execute();
    }

    /**
     * This is starter function to prepare the acoustic model & dictionary file.
     * You need to set the language model with updateVocabulary, updateGrammar or updateLmFile before start recognizer.
     * @param rapidPreparationListener The listener to add new configuration. This param is nullable.
     */
    public void prepareRapidSphinx(final RapidPreparationListener rapidPreparationListener) {
        if (ContextCompat.checkSelfPermission(this.context,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("Perrmission record not granted!");
            return;
        }
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected void onPreExecute() {
                System.out.println("Preparing RapidSphinx!");
            }
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assetsDir = new Assets(context);
                    File assetDir = assetsDir.syncAssets();
                    SpeechRecognizerSetup speechRecognizerSetup = SpeechRecognizerSetup.defaultSetup();
                    speechRecognizerSetup.setAcousticModel(new File(assetDir, "en-us-ptm"));
                    speechRecognizerSetup.setDictionary(new File(assetDir, "cmudict-en-us.dict"));
                    config = speechRecognizerSetup.getRecognizer().getDecoder().getConfig();
                    config.setFloat("-samprate", sampleRate);
                    config.setFloat("-vad_threshold", vadThreshold);
                    config.setFloat("-lw", 6.5);
                    if (rapidPreparationListener != null) {
                        rapidPreparationListener.rapidPreExecute(config);
                    }
                    if (rawLogAvailable) {
                        config.setString("-rawlogdir", assetDir.getPath());
                    }
                    rapidRecognizer = new RapidRecognizer(assetDir, config, rapidSphinxListener);
                    rapidRecognizerTemp = new RapidRecognizer(assetDir, config, rapidSphinxListener);
                    rapidRecognizer.addRapidListener(RapidSphinx.this);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    if (rapidPreparationListener != null) {
                        rapidPreparationListener.rapidPostExecute(false);
                    }
                }
                return null;
            }
            @Override
            protected void onPostExecute(Exception e) {
                System.out.println("RapidSphinx is ready!");
                if (rapidPreparationListener != null) {
                    rapidPreparationListener.rapidPostExecute(true);
                }
            }
        }.execute();
    }

    /**
     * This is starter function to prepare the acoustic model, dictionary, & language model file.
     * You can start recognizer after call this function.
     * @param gramOrLM Path of the language model file like jsgf grammar, arpa, bin, lm or dmp
     * @param acousticModel Path of the acoustioc model
     * @param dictionary Path of dictionary file
     * @param rapidPreparationListener The listener to add new configuration. This param is nullable.
     */
    public void prepareRapidSphinx(@NonNull final File gramOrLM, @Nullable final File acousticModel, @Nullable final File dictionary, final RapidPreparationListener rapidPreparationListener) {
        if (ContextCompat.checkSelfPermission(this.context,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("Perrmission record not granted!");
            return;
        }
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected void onPreExecute() {
                System.out.println("Preparing RapidSphinx!");
            }
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assetsDir = new Assets(context);
                    File assetDir = assetsDir.syncAssets();
                    SpeechRecognizerSetup speechRecognizerSetup = SpeechRecognizerSetup.defaultSetup();
                    config = speechRecognizerSetup.getRecognizer().getDecoder().getConfig();
                    if (acousticModel != null) {
                        config.setString("-hmm", acousticModel.getPath());
                    } else {
                        config.setString("-hmm", new File(assetDir, "en-us-ptm").getPath());
                    }
                    if (dictionary != null) {
                        config.setString("-dict", dictionary.getPath());
                    } else {
                        config.setString("-dict", new File(assetDir, "cmudict-en-us.dict").getPath());
                    }
                    config.setFloat("-samprate", sampleRate);
                    config.setFloat("-vad_threshold", vadThreshold);
                    config.setFloat("-lw", 6.5);
                    if (rapidPreparationListener != null) {
                        rapidPreparationListener.rapidPreExecute(config);
                    }
                    if (rawLogAvailable) {
                        config.setString("-rawlogdir", assetDir.getPath());
                    }
                    if (gramOrLM.isFile()) {
                        if (gramOrLM.getPath().endsWith(".gram")) {
                            config.setString("-jsgf", gramOrLM.getPath());
                        } else if (gramOrLM.getPath().endsWith(".arpa") ||
                                gramOrLM.getPath().endsWith(".lm") ||
                                gramOrLM.getPath().endsWith(".bin")||
                                gramOrLM.getPath().endsWith(".dmp")) {
                            config.setString("-lm", gramOrLM.getPath());
                        } else {
                            Log.e("RapidSphinx", gramOrLM.getPath() + " is not a language model file.");
                        }
                    } else {
                        Log.e("RapidSphinx", gramOrLM.getPath() + " is not a file.");
                    }
                    rapidRecognizer = new RapidRecognizer(assetDir, config, rapidSphinxListener);
                    rapidRecognizerTemp = new RapidRecognizer(assetDir, config, rapidSphinxListener);
                    rapidRecognizer.addRapidListener(RapidSphinx.this);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    if (rapidPreparationListener != null) {
                        rapidPreparationListener.rapidPostExecute(false);
                    }
                }
                return null;
            }
            @Override
            protected void onPostExecute(Exception e) {
                System.out.println("RapidSphinx is ready!");
                if (rapidPreparationListener != null) {
                    rapidPreparationListener.rapidPostExecute(true);
                }
            }
        }.execute();
    }

    /**
     * This is starter function to prepare the acoustic model, dictionary, & language model file with full vocabulary.
     * You can start recognizer after call this function.
     * Full language model will reduce the accuracy. So, you need to consider before use this feature.
     * @param rapidPreparationListener The listener to add new configuration. This param is nullable.
     */
    public void prepareRapidSphinxFullLM(final RapidPreparationListener rapidPreparationListener) {
        if (ContextCompat.checkSelfPermission(this.context,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("Perrmission record not granted!");
            return;
        }
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected void onPreExecute() {
                System.out.println("Preparing RapidSphinx!");
            }
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assetsDir = new Assets(context);
                    File assetDir = assetsDir.syncAssets();
                    SpeechRecognizerSetup speechRecognizerSetup = SpeechRecognizerSetup.defaultSetup();
                    speechRecognizerSetup.setAcousticModel(new File(assetDir, "en-us-ptm"));
                    speechRecognizerSetup.setDictionary(new File(assetDir, "cmudict-en-us.dict"));
                    config = speechRecognizerSetup.getRecognizer().getDecoder().getConfig();
                    config.setFloat("-samprate", sampleRate);
                    config.setFloat("-vad_threshold", vadThreshold);
                    config.setFloat("-lw", 6.5);
                    if (rapidPreparationListener != null) {
                        rapidPreparationListener.rapidPreExecute(config);
                    }
                    if (rawLogAvailable) {
                        config.setString("-rawlogdir", assetDir.getPath());
                    }
                    rapidRecognizer = new RapidRecognizer(assetDir, config, rapidSphinxListener);
                    rapidRecognizerTemp = new RapidRecognizer(assetDir, config, rapidSphinxListener);
                    rapidRecognizer.getRapidDecoder().setLmFile(SEARCH_ID, new File(assetDir, "en-us.lm.bin").getPath());
                    rapidRecognizer.addRapidListener(RapidSphinx.this);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    if (rapidPreparationListener != null) {
                        rapidPreparationListener.rapidPostExecute(false);
                    }
                }
                return null;
            }
            @Override
            protected void onPostExecute(Exception e) {
                System.out.println("RapidSphinx is ready!");
                if (rapidPreparationListener != null) {
                    rapidPreparationListener.rapidPostExecute(true);
                }
            }
        }.execute();
    }

    /**
     * Start the recognizer with timout in miliseconds
     * @param timeOut Time in miliseconds
     */
    public void startRapidSphinx(int timeOut) {
        if (ContextCompat.checkSelfPermission(this.context,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("Perrmission record not granted!");
        } else {
            scores.clear();
            hypArr.clear();
            rapidRecognizer.startRapidListening(SEARCH_ID, timeOut);
        }
    }

    /**
     * Update new vocabulary with new words in string.
     * This func will replace old language model with new language model.
     * @param words New words in string
     * @param oovWords Out-of-vocabulary: word in this array string will be ??? if recognized
     * @param rapidCompletionListener The listener to give feedback vocabulary updated. Nullable
     */
    public void updateVocabulary(@NonNull final String words, @Nullable final String[] oovWords, @Nullable final RapidCompletionListener rapidCompletionListener) {
        new AsyncTask<Void, Void, Exception>(){
            @Override
            protected void onPreExecute() {
                RapidSphinx.this.originalWords.clear();
                RapidSphinx.this.oovWords.clear();
                RapidSphinx.this.vocabularies.clear();
                System.out.println("Updating vocabulary!");
            }
            @Override
            protected Exception doInBackground(Void... params) {
                RapidSphinx.this.originalWords = Arrays.asList(words.trim().split(" "));
                RapidSphinx.this.oovWords = Arrays.asList(oovWords);
                RapidSphinx.this.vocabularies.addAll(RapidSphinx.this.originalWords);
                if (oovWords != null) {
                    RapidSphinx.this.vocabularies.addAll(RapidSphinx.this.oovWords);
                }
                generateDictonary(new HashSet<String>(RapidSphinx.this.vocabularies).toArray(new String[0]));
                generateNGramModel(new HashSet<String>(RapidSphinx.this.vocabularies).toArray(new String[0]));
                return null;
            }
            @Override
            protected void onPostExecute(Exception e) {
                System.out.println("Vocabulary updated!");
                if (rapidCompletionListener != null) {
                    rapidCompletionListener.rapidCompletedProcess();
                }
                if (rapidSphinxListener != null) {
                    rapidSphinxListener.rapidSphinxUnsupportedWords(unsupportedWords);
                }
            }
        }.execute();
    }

    /**
     * Update new vocabulary with new words in array string.
     * This func will replace old language model with new language model.
     * @param words New words in string
     * @param oovWords Out-of-vocabulary: word in this array string will be ??? if recognized
     * @param rapidCompletionListener The listener to give feedback vocabulary updated. Nullable
     */
    public void updateVocabulary(final String[] words, @Nullable final String[] oovWords, @Nullable final RapidCompletionListener rapidCompletionListener) {
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected void onPreExecute() {
                RapidSphinx.this.originalWords.clear();
                RapidSphinx.this.oovWords.clear();
                RapidSphinx.this.vocabularies.clear();
                System.out.println("Updating vocabulary!");
            }
            @Override
            protected Exception doInBackground(Void... params) {
                RapidSphinx.this.originalWords = Arrays.asList(words);
                RapidSphinx.this.oovWords = Arrays.asList(oovWords);
                RapidSphinx.this.vocabularies.addAll(RapidSphinx.this.originalWords);
                if (oovWords != null) {
                    RapidSphinx.this.vocabularies.addAll(RapidSphinx.this.oovWords);
                }
                generateDictonary(new HashSet<String>(RapidSphinx.this.vocabularies).toArray(new String[0]));
                generateNGramModel(new HashSet<String>(RapidSphinx.this.vocabularies).toArray(new String[0]));
                return null;
            }
            @Override
            protected void onPostExecute(Exception e) {
                System.out.println("Vocabulary updated!");
                if (rapidCompletionListener != null) {
                    rapidCompletionListener.rapidCompletedProcess();
                }
                if (rapidSphinxListener != null) {
                    rapidSphinxListener.rapidSphinxUnsupportedWords(unsupportedWords);
                }
            }
        }.execute();
    }

    /**
     * Update new grammar with string.
     * This func will replace old jsgf grammar with new jsgf grammar.
     * @param grammarStr New words in string
     * @param oogWords Out-of-grammar: word in this array string will be ??? if recognized
     * @param rapidCompletionListener The listener to give feedback grammar updated. Nullable
     */
    public void updateGrammar(final String grammarStr, @Nullable final String[] oogWords, @Nullable final RapidCompletionListener rapidCompletionListener) {
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected void onPreExecute() {
                RapidSphinx.this.originalWords.clear();
                RapidSphinx.this.oovWords.clear();
                RapidSphinx.this.vocabularies.clear();
                System.out.println("Updating vocabulary!");
            }
            @Override
            protected Exception doInBackground(Void... params) {
                RapidSphinx.this.originalWords = Arrays.asList(grammarStr.trim().split(" "));
                RapidSphinx.this.oovWords = Arrays.asList(oogWords);
                RapidSphinx.this.vocabularies.addAll(RapidSphinx.this.originalWords);
                if (oogWords != null) {
                    RapidSphinx.this.vocabularies.addAll(RapidSphinx.this.oovWords);
                }
                generateDictonary(new HashSet<String>(RapidSphinx.this.vocabularies).toArray(new String[0]));
                rapidRecognizer.getRapidDecoder().setJsgfString(SEARCH_ID, grammarStr);
                return null;
            }
            @Override
            protected void onPostExecute(Exception e) {
                System.out.println("Vocabulary updated!");
                if (rapidCompletionListener != null) {
                    rapidCompletionListener.rapidCompletedProcess();
                }
            }
        }.execute();
    }

    /**
     * Update new jsgf grammar (NonNull) & dictionary (Nullable).
     * Replace old jsgf grammar with new jsgf grammar.
     * Replace old dictionary with new dictionary.
     * @param jsgf Path of jsgf grammar file.
     * @param dictionary Path of dictionary file.
     * @param rapidCompletionListener The listener to give feedback grammar updated. Nullable
     */
    public void updateGrammar(final File jsgf, @Nullable final File dictionary, @Nullable final RapidCompletionListener rapidCompletionListener) {
        if (jsgf.isFile()) {
            new AsyncTask<Void, Void, Exception>() {
                @Override
                protected void onPreExecute() {
                    System.out.println("Updating vocabulary!");
                }
                @Override
                protected Exception doInBackground(Void... params) {
                    if (dictionary != null) {
                        if (dictionary.isFile()) {
                            rapidRecognizer.getRapidDecoder().loadDict(dictionary.getPath(), null, "dict");
                        } else {
                            Log.e("RapidSphinx", dictionary.getPath() + " is not a dictionary file");
                        }
                    }
                    rapidRecognizer.getRapidDecoder().setJsgfFile(SEARCH_ID, jsgf.getPath());
                    return null;
                }
                @Override
                protected void onPostExecute(Exception e) {
                    System.out.println("Vocabulary updated!");
                    if (rapidCompletionListener != null) {
                        rapidCompletionListener.rapidCompletedProcess();
                    }
                }
            }.execute();
        } else {
            Log.e("RapidSphinx", jsgf.getPath() + " is not a grammar file");
        }
    }

    /**
     * Update new language model (NonNull) & dictionary (Nullable).
     * Replace old language model with new language model.
     * Replace old dictionary with new dictionary.
     * @param lmFile Path of language model file.
     * @param dictionary Path of dictionary file.
     * @param rapidCompletionListener The listener to give feedback grammar updated. Nullable
     */
    public void updateLmFile(final File lmFile, @Nullable final File dictionary, @Nullable final RapidCompletionListener rapidCompletionListener) {
        if (lmFile.isFile()) {
            new AsyncTask<Void, Void, Exception>() {
                @Override
                protected void onPreExecute() {
                    System.out.println("Updating vocabulary!");
                }
                @Override
                protected Exception doInBackground(Void... params) {
                    if (dictionary != null) {
                        if (dictionary.isFile()) {
                            rapidRecognizer.getRapidDecoder().loadDict(dictionary.getPath(), null, "dict");
                        } else {
                            Log.e("RapidSphinx", dictionary.getPath() + " is not a dictionary file");
                        }
                    }
                    rapidRecognizer.getRapidDecoder().setLmFile(SEARCH_ID, lmFile.getPath());
                    return null;
                }
                @Override
                protected void onPostExecute(Exception e) {
                    System.out.println("Vocabulary updated!");
                    if (rapidCompletionListener != null) {
                        rapidCompletionListener.rapidCompletedProcess();
                    }
                }
            }.execute();
        } else {
            Log.e("RapidSphinx", lmFile.getPath() + " is not a language model file");
        }
    }

    /**
     * Get RapidSphinx Decoder
     * @return
     */
    public Decoder getDecoder() {
        return rapidRecognizer.getRapidDecoder();
    }

    /**
     * Get The Recorder class.
     * Get audio record and play from this class.
     * @return
     */
    public RapidRecorder getRapidRecorder() {
        return rapidRecognizer.getRapidRecorder();
    }

    /**
     * Add listener to RapidSphinx
     * @param rapidSphinxListener
     */
    public void addListener(RapidSphinxListener rapidSphinxListener) {
        this.rapidSphinxListener = rapidSphinxListener;
    }

    /**
     * Remove the listener
     */
    public void removeListener() {
        this.rapidSphinxListener = null;
    }

    /**
     * Get the language model path. This languge model is generated from RapidSphinx.
     * @return
     */
    public File getLanguageModelPath() {
        File lmFile = new File(assetDir, "arpas/"+ SEARCH_ID +"-new.arpa");
        return lmFile;
    }

    /**
     * Get dictionary path. This dictionary generated from RapidSphinx.
     * @return
     */
    public File getDictonaryFile() {
        File dictFile = new File(assetDir,  "arpas/"+ SEARCH_ID +".dict");
        return dictFile;
    }

    /**
     * Check if RapidSphinx support for raw log
     * @return
     */
    public boolean isRawLogAvailable() {
        return rawLogAvailable;
    }

    /**
     * Get raw log directory
     * @return
     */
    public String getRawLogDirectory() {
        return assetDir.getPath();
    }

    /**
     * Set raw log directory. Default is false
     * @param rawLogAvailable
     */
    public void setRawLogAvailable(boolean rawLogAvailable) {
        this.rawLogAvailable = rawLogAvailable;
    }

    /**
     * Get the vad threshold.
     * @return
     */
    public double getVadThreshold() {
        return vadThreshold;
    }

    /**
     * Set the vadThreshold.
     * @param vadThreshold Default is 3.0. Maximum value is 4.0
     */
    public void setVadThreshold(float vadThreshold) {
        this.vadThreshold = vadThreshold;
    }

    /**
     * Stop the recognizer and release the release the recorder.
     */
    public void stop() {
        rapidRecognizer.stopRapid();
        rapidRecognizer.shutdownRapid();
    }

    /**
     * Get the segment iterator of hypotesis
     * @return
     */
    public SegmentIterator getSegmentIterator() {
        return rapidRecognizer.getRapidDecoder().seg().iterator();
    }

    @Override
    public void onBeginningOfSpeech() {
        if (rapidSphinxListener != null) {
            rapidSphinxListener.rapidSphinxDidSpeechDetected();
        }
    }

    @Override
    public void onEndOfSpeech() {
        stop();
        if (rapidSphinxListener != null) {
            rapidSphinxListener.rapidSphinxDidStop("End of Speech!", 200);
        }
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            if (rapidSphinxListener != null) {
                rapidSphinxListener.rapidSphinxPartialResult(hypothesis.getHypstr());
            }
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            SegmentIterator segmentIterator = rapidRecognizer.getRapidDecoder().seg().iterator();
            String hypStr = "";
            while (segmentIterator.hasNext()) {
                Segment segment = segmentIterator.next();
                double score =  rapidRecognizer.getRapidDecoder().getLogmath().exp(segment.getProb());
                if (!segment.getWord().contains("<") && !segment.getWord().contains(">") &&
                        !segment.getWord().contains("[") && !segment.getWord().contains("]")) {
                    if (originalWords.contains(segment.getWord())) {
                        hypStr += segment.getWord() + " ";
                    } else {
                        hypStr += "??? ";
                    }
                    scores.add(score);
                    hypArr.add(segment.getWord());
                }
            }
            if (rapidSphinxListener != null) {
                rapidSphinxListener.rapidSphinxFinalResult(hypStr.trim(), hypArr, scores);
            }
        }
    }

    @Override
    public void onError(Exception e) {
        stop();
        if (rapidSphinxListener != null) {
            rapidSphinxListener.rapidSphinxDidStop(e.getMessage(), 500);
        }
    }

    @Override
    public void onTimeout() {
        stop();
        if (rapidSphinxListener != null) {
            rapidSphinxListener.rapidSphinxDidStop("Speech timed out!", 522);
        }
    }
}
