package com.vtsman.gbemu;

import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

/**
 * Created by Spencer on 7/14/18.
 */
public class AudioRingBuffer {
    private byte[] data;
    private int base = 0;
    private int len = 0;
    public AudioRingBuffer(int bitrate, float duration){
        data = new byte[(int)(bitrate * duration)];
    }

    public void add(byte[] input){
        for(int i = 0; i < input.length; i++){
            int index = (i + base + len) % (data.length);
            data[index] = input[i];
        }
        len += input.length;
        if(len > data.length){
            System.err.println("Warning: AudioRingBuffer overrun");
        }
    }

    public void add(byte b){
        int index = (base + len) % (data.length);
        data[index] = b;
        len++;
        if(len > data.length){
            System.err.println("Warning: AudioRingBuffer overrun");
        }
    }

    public void queueSample(SourceDataLine line){
        int l1 = len > (data.length - base) ? (data.length - base) : len;
        line.write(data, base, l1);
        if(l1 != len){
            line.write(data, 0, len - l1);
        }
        base = (base + len) % (data.length);
        len = 0;
    }
}
