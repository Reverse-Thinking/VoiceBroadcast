package com.github.voice.broadcast;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SoundPlayUtils {

    private static String TAG = SoundPlayUtils.class.getSimpleName();

    private static SoundPool mSoundPlayer;

    private static SoundPlayUtils soundPlayUtils = new SoundPlayUtils();

    public static SoundPlayUtils newInstance() {
        return soundPlayUtils;
    }

    private SoundPlayUtils() {
        init();
    }

    private HashMap<String, Sound> mAmounts = new HashMap<>();
    private final static int START_PLAY = 1;
    private Handler mVoiceHandler;
    private boolean mPlaying;

    private static String[] units = {"qian", "bai", "shi"};
    private static final String WAN = "wan";
    private static final String YI = "yii";
    private static final String DOT = "dot";
    private static final String MERCHANT = "merchant";
    private static final String YUAN = "yuan";

    private Queue<Sound> mSoundQueue = new LinkedList<>();

    private Sound mCurrentSound;

    private AssetManager mAssetManager;

    private void init() {
        HandlerThread handlerThread = new HandlerThread( "handler-thread") ;
        handlerThread.start();
        mVoiceHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case START_PLAY:
                        soundPlayUtils.playNextSound();
                        break;
                    default:
                        break;
                }
            }
        };
        mAmounts.put("merchant", new Sound(1812, "merchant"));
        mAmounts.put("1", new Sound(416, "yi"));
        mAmounts.put("2", new Sound(370, "er"));
        mAmounts.put("3", new Sound(452, "san"));
        mAmounts.put("4", new Sound(443, "si"));
        mAmounts.put("5", new Sound(432, "wu"));
        mAmounts.put("6", new Sound(383, "liu"));
        mAmounts.put("7", new Sound(404, "qi"));
        mAmounts.put("8", new Sound(314, "ba"));
        mAmounts.put("9", new Sound(428, "jiu"));
        mAmounts.put("shi", new Sound(439, "shi"));
        mAmounts.put("bai", new Sound(320, "bai"));
        mAmounts.put("qian", new Sound(433, "qian"));
        mAmounts.put("wan", new Sound(384, "wan"));
        mAmounts.put("yii", new Sound(394, "yii"));
        mAmounts.put("dot", new Sound(312, "dot"));
        mAmounts.put("0", new Sound(465, "ling"));
        mAmounts.put("yuan", new Sound(486, "yuan"));
    }

    public void playNumber(String price) {
        if (!isLegalPrice(price)) {
            return;
        }
        List<String> speech = new LinkedList<>();
        convertPrice(speech, price);
        playSeqSounds(speech);
    }

    private void playSeqSounds(List<String> soundsToPlay) {
        int length = soundsToPlay.size();
        Log.i(TAG, "length start:" + length);
        if (mSoundQueue.size() == 0 && !mPlaying) {
            for (int j = 0; ; j++) {
                Log.i(TAG, "length if for:" + length);
                if (j >= length) {
                    if (mVoiceHandler != null) {
                        mVoiceHandler.sendEmptyMessage(START_PLAY);
                    }
                    return;
                }
                String str = soundsToPlay.get(j);
                this.mSoundQueue.add(mAmounts.get(str));
                Log.i(TAG, "mSoundQueue.add: " + str + " :ThreadName" + Thread.currentThread());
            }
        } else {
            for (int j = 0; ; j++) {
                Log.i(TAG, "length if for:" + length);
                if (j >= length) {
                    return;
                }
                String str = soundsToPlay.get(j);
                this.mSoundQueue.add(mAmounts.get(str));
                Log.i(TAG, "mSoundQueue.add: " + str + " :ThreadName" + Thread.currentThread());
            }
        }
    }

    private void playNextSound() {
        if (!mSoundQueue.isEmpty()) {
            mCurrentSound = mSoundQueue.poll();
            loadSound();
        } else {
            mPlaying = false;
            Log.i(TAG, "startPlayer end" + " :ThreadName" + Thread.currentThread());
        }
    }

    @SuppressWarnings("deprecation")
    private void loadSound() {
        if (mSoundPlayer == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attr = new AudioAttributes.Builder().
                        setLegacyStreamType(AudioManager.STREAM_MUSIC).build();
                mSoundPlayer = new SoundPool.Builder().setAudioAttributes(attr).setMaxStreams(3).build();
            } else {
                mSoundPlayer = new SoundPool(3, AudioManager.STREAM_MUSIC, 1);
            }
            mAssetManager =  MyApplication.getAppContext().getAssets();
            mSoundPlayer.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    Log.i(TAG, "onLoadComplete sampleId: " + sampleId);
                    if (status == 0) {
                        mPlaying = true;
                        int streamID = startPlayer(sampleId);
                        // play failed
                        if (streamID == 0) {
                            Log.i(TAG, "streamID == 0");
                            releaseSound();
                            playNextSound();
                        } else {
                            try {
                                Thread.sleep(mCurrentSound.duration);
                                if ("yuan".equals(mCurrentSound.soundName) || "diancan".equals(mCurrentSound.soundName)) {
                                    releaseSound();
                                }
                                playNextSound();
                            } catch (InterruptedException e) {
                                mPlaying = false;
                            }
                        }
                    } else {
                        // load failed
                        Log.i(TAG, "status != 0");
                        releaseSound();
                    }
                }
            });
        }
        try {
            AssetFileDescriptor afd = mAssetManager.openFd(mCurrentSound.soundName + ".mp3");
            mSoundPlayer.load(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength(), 1);
        } catch (Exception e) {
            Log.i(TAG, "AssetFileDescriptor load IOException");
            playNextSound();
        }
    }

    private void releaseSound() {
        Log.i(TAG, "releaseSound");
        mSoundPlayer.release();
        mSoundPlayer = null;
    }

    /**
     * 播放声音
     *
     * @param soundID
     */
    public int startPlayer(int soundID) {
        return mSoundPlayer.play(soundID, 1, 1, 0, 0, 1);
    }

    private void convertPrice(List<String> speech, String price) {
        speech.add(MERCHANT);

        String[] temp = price.split("\\.");
        if (temp.length == 2) {
            if (temp[1].length() == 1 && "0".equals(temp[1])) {
                price = temp[0];
            } else if (temp[1].length() == 2) {
                char[] chars = temp[1].toCharArray();
                if (chars[0] == '0' && chars[1] == '0') {
                    price = temp[0];
                } else if (chars[0] != '0' && chars[1] == '0') {
                    temp[1] = String.valueOf(chars[0]);
                    price = temp[0] + "." + chars[0];
                }
            }
        }

        String[] strings = price.split("\\.");

        String integerString = strings[0];
        int startPosition = 0;

        //处理 亿位 及以上
        if (integerString.length() > 8) {
            String oneHundredMillionString = integerString.substring(0, integerString.length() - 8);
            startPosition = integerString.length() - 8;
            speech.addAll(unitPrice(oneHundredMillionString));
            speech.add(YI);
        }

        //处理 亿位 以下 至 万位
        if (integerString.length() > 4) {
            String tenHundredString = integerString.substring(startPosition, integerString.length() - 4);
            speech.addAll(unitPrice(tenHundredString));
            startPosition = integerString.length() - 4;
            speech.add(WAN);
        }

        //处理 万位 以下
        String finalString = integerString.substring(startPosition, integerString.length());
        if ("0".equals(finalString)) {
            speech.add("0");
        } else {
            speech.addAll(unitPrice(finalString));
        }

        //处理小数点后的读音
        if (strings.length == 2) {
            speech.add(DOT);
            int decimalLength = strings[1].length();
            for (int i = 0; i < decimalLength; i++) {
                speech.add(String.valueOf(strings[1].charAt(i)));
            }
        }

        speech.add(YUAN);
    }

    private List<String> unitPrice(String simplePrice) {
        List<String> result = new LinkedList<>();
        int length = simplePrice.length();

        //当为2位数且第一位为1时,1不用读
        if (length == 2 && simplePrice.charAt(0) == '1') {
            result.add(units[2]);
            char lastChar = simplePrice.charAt(1);
            if (lastChar != '0') {
                result.add(String.valueOf(lastChar));
            }
            return result;
        }

        boolean readZero = simplePrice.charAt(0) == '0';
        int unitPos = 4 - length;
        if (simplePrice.length() <= 4) {
            for (int i = 0; i < length; i++) {
                if (simplePrice.charAt(i) != '0') {
                    if (readZero) {
                        result.add("0");
                    }
                    readZero = false;
                    result.add(String.valueOf(simplePrice.charAt(i)));
                    //最后一个数字不用读单位
                    if (i != length - 1)
                        result.add(units[unitPos]);
                } else {
                    readZero = true;
                }
                unitPos++;
            }
        }
        return result;
    }

    private boolean isLegalPrice(String price) {
        boolean res = true;
        int len = price.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isDigit(price.charAt(i)) && price.charAt(i) != '.') {
                res = false;
                break;
            }
        }
        if (price.split("\\.")[0].length() > 12) {
            res = false;
        }
        if (price.length() >= 2 && price.charAt(0) == '0' && price.charAt(1) != '.') {
            res = false;
        }
        return res;
    }

    private final class Sound {
        private int duration;
        private String soundName;
        private Sound(int time, String soundName) {
            this.duration = time;
            this.soundName = soundName;
        }
    }
}