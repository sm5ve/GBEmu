package com.vtsman.gbemu;
import javax.sound.sampled.*;

/**
 * Created by Spencer on 7/14/18.
 */
public class Speaker {
    public static final int SAMPLERATE = 22050;
    public static final int SAMPLESIZE = 16;
    public static final int CHANNELS = 1;
    public static final boolean SIGNED = true;
    public static final boolean BIGENDIAN = true;

    private static AudioFormat format;
    private static DataLine.Info info;
    private static SourceDataLine auline;

    static {
        format = new AudioFormat(SAMPLERATE, SAMPLESIZE, CHANNELS, SIGNED, BIGENDIAN);
        info = new DataLine.Info(SourceDataLine.class, format);
        initSoundSystem();
    }
    private static void initSoundSystem(){
        try {
            // Get line to write data to
            auline = (SourceDataLine) AudioSystem.getLine(info);
            auline.open(format);
            auline.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Speaker(){

    }

    public void playQueuedAudio(){

    }
}
