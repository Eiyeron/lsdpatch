package kitEditor;/* Copyright (C) 2001-2011 by Johan Kotlinski

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE. */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.sound.sampled.*;
import javax.swing.*;

class Sample {
    private final String name;
    private final float[] buf;
    private int readPos;
    private int ditherAmount;
    private boolean ditherable;

    private Sample(float[] iBuf, String iName, boolean ditherable) {
        buf = iBuf;
        name = iName;
        ditherAmount = 0;
        this.ditherable = ditherable;
    }

    String getName() {
        return name;
    }

    int length() {
        return buf.length;
    }

    void seekStart() {
        readPos = 0;
    }

    int getDitherAmount()
    {
        return ditherAmount;
    }

    void setDitherAmount(int newAmount)
    {
        if(!ditherable)
        {
            return;
        }

        ditherAmount = newAmount;
    }

    boolean isDitherable()
    {
        return ditherable;
    }

    float read() {
        return buf[readPos++];
    }

    // ------------------
    static private float nibbleToFloat(int nibble)
    {
        nibble = (nibble + 256)%16;
        return (float)(nibble)/15.f;
    }

    // TODO use Javax's Audio API.
    static Sample createFromNibbles(byte[] nibbles, String name) {
        float[] buf = new float[nibbles.length * 2];
        for (int nibbleIt = 0; nibbleIt < nibbles.length; ++nibbleIt) {
            buf[2 * nibbleIt] = nibbleToFloat((nibbles[nibbleIt] & 0xf0)>>4);
            buf[2 * nibbleIt + 1] = nibbleToFloat(nibbles[nibbleIt]);
        }
        return new Sample(buf, name, false);
    }

    // ------------------

    static Sample createFromWav(File file, boolean canDither) {
        try {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
            AudioFormat outputFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_FLOAT,
                    11468,
                    32,
                    1,
                    4,
                    11468,
                    true);
            AudioInputStream outputStream = AudioSystem.getAudioInputStream(outputFormat, inputStream);

            ByteBuffer byteBuffer = ByteBuffer.allocate((int)inputStream.getFrameLength());
            outputStream.read(byteBuffer.array());
            FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
            float[] outputBuffer= new float[floatBuffer.limit()];
            floatBuffer.get(outputBuffer);
            for (int i = 0; i < outputBuffer.length; i++) {
                outputBuffer[i] = outputBuffer[i]/2.f + 0.5f;
            }
            return new Sample(outputBuffer, file.getName(), canDither);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    byte[] to4BitDepthBytes()
    {
        byte[] outputBuffer = new byte[length()];
        for (int i = 0; i < length(); i++) {
            outputBuffer[i] = (byte)((int)(buf[i]*15.f)<<4);
        }
        return outputBuffer;
    }

    byte[] toLSDjFormat()
    {
        int sampleLength = length();
        // Trims the end of the sample to make it a multiple of 0x10.
        sampleLength -= sampleLength % 0x10;
        byte[] outputBuffer = new byte[length()/2];
        int outputBufferCursor = 0;

        int addedBytes = 0;

        int intermediaBuffer[] = new int[32];
        int outputCounter = 0;
        for (int i = 0; i < sampleLength; i++) {
            float frame = buf[i];
            if (isDitherable()) {
                float ditherAmount = getDitherAmount()/15.f;
                frame += Math.random() * ditherAmount - ditherAmount/2.f;
            }

            int result = (int)(frame * 15.f);
            intermediaBuffer[outputCounter] = result;

            if (outputCounter == 31) {
                for (int j = 0; j != 32; j += 2) {
                    outputBuffer[outputBufferCursor++] = (byte) (intermediaBuffer[j] * 0x10 + intermediaBuffer[j + 1]);
                }
                outputCounter = -1;
                addedBytes += 0x10;
            }
            outputCounter++;
        }

        return outputBuffer;

    }

    // ------------------

    void writeToWav(File f) {
        try {
            RandomAccessFile wavFile = new RandomAccessFile(f, "rw");

            int payloadSize = buf.length;
            int fileSize = buf.length + 0x2c;
            int waveSize = fileSize - 8;

            byte[] header = {
                    0x52, 0x49, 0x46, 0x46,  // RIFF
                    (byte) waveSize,
                    (byte) (waveSize >> 8),
                    (byte) (waveSize >> 16),
                    (byte) (waveSize >> 24),
                    0x57, 0x41, 0x56, 0x45,  // WAVE
                    // --- fmt chunk
                    0x66, 0x6D, 0x74, 0x20,  // fmt
                    16, 0, 0, 0,  // fmt size
                    1, 0,  // pcm
                    1, 0,  // channel count
                    (byte) 0xcc, 0x2c, 0, 0,  // freq (11468 hz)
                    (byte) 0xcc, 0x2c, 0, 0,  // avg. bytes/sec
                    1, 0,  // block align
                    8, 0,  // bits per sample
                    // --- data chunk
                    0x64, 0x61, 0x74, 0x61,  // data
                    (byte) payloadSize,
                    (byte) (payloadSize >> 8),
                    (byte) (payloadSize >> 16),
                    (byte) (payloadSize >> 24)
            };

            wavFile.write(header);

            byte[] unsigned = new byte[buf.length];
            for (int it = 0; it < buf.length; ++it) {
                unsigned[it] = (byte) ((int) buf[it] + 0x80);
            }
            wavFile.write(unsigned);
            wavFile.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "File error : " +e.getCause(),
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}

