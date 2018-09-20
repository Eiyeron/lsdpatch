package structures;

import kitEditor.Sample;
import kitEditor.Sound;
import utils.RomUtilities;

import javax.sound.sampled.LineUnavailableException;

public class LSDJKit {


    private byte romImage[] = null;
    private int pageIndex = -1;
    private Sample[] samples = new Sample[RomUtilities.KIT_MAX_NUM_SAMPLES];

    public LSDJKit() {
    }

    public LSDJKit(byte romImage[], int pageIndex) {
        this.romImage = romImage;
        this.pageIndex = pageIndex;
        updateSamples();
    }

    public void setRomImage(byte romImage[]) {
        this.romImage = romImage;
    }

    private void updateSamples() {
        for (int i = 0; i <RomUtilities.KIT_MAX_NUM_SAMPLES; i++) {
            byte[] nibbles = get4BitSamples(i, false);
            if (nibbles != null) {
                samples[i] = Sample.createFromNibbles(nibbles, getRomSampleName(i));
            }
        }
    }

    public Sample getSamples(int index) {
        return samples[index];
    }

    private String getRomSampleName(int index) {
        int offset = getPageStartAddress() + 0x22 + index * 3;
        String name = "";
        name += (char) romImage[offset++];
        name += (char) romImage[offset++];
        name += (char) romImage[offset];
        return name;
    }

    private int getPageStartAddress() {
        return pageIndex * RomUtilities.BANK_SIZE;
    }

    public int getUsedSize() {
        int totalSize = 0;
        for (int i = 0; i <RomUtilities.KIT_MAX_NUM_SAMPLES; i++) {
            int sampleEnd = getSampleEnd(i);
            if (sampleEnd == 0) {
                break;
            }
            totalSize = sampleEnd;
        }
        return totalSize;
    }

    public int getSampleStart(int sampleIndex) {
        if (sampleIndex == 0) {
            return 0x4060;
        }
        return getSampleEnd(sampleIndex - 1);
    }

    public int getSampleEnd(int sampleIndex) {
        int sampleStartPosition = getPageStartAddress() + sampleIndex * 2 + 2;
        return (0xff & romImage[sampleStartPosition]) | ((0xff & romImage[sampleStartPosition + 1]) << 8);
    }

    public byte[] get4BitSamples(int sampleIndex, boolean halfSpeed) {
        int start = getSampleStart(sampleIndex);
        int stop = getSampleEnd(sampleIndex);

        if (stop <= start) {
            return null;
        }

        byte[] arr;
        if (halfSpeed) {
            arr = new byte[(stop - start) * 2];
            for (int i = start; i < stop; ++i) {
                arr[(i - start) * 2] = romImage[getPageStartAddress() - RomUtilities.BANK_SIZE + i];
                arr[(i - start) * 2 + 1] = romImage[getPageStartAddress() - RomUtilities.BANK_SIZE + i];
            }

        } else {
            arr = new byte[stop - start];
            for (int i = start; i < stop; ++i) {
                arr[i - start] = romImage[getPageStartAddress() - RomUtilities.BANK_SIZE + i];
            }
        }
        return arr;
    }

    public String getName() {
        return RomUtilities.getKitName(romImage, pageIndex);
    }
}
