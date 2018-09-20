package structures;

import kitEditor.Sample;
import kitEditor.Sound;
import utils.RomUtilities;

import javax.sound.sampled.LineUnavailableException;

public class LSDJKit {


    private byte romImage[] = null;
    private int pageIndex = -1;
    private Sample[] samples = new Sample[16];

    public LSDJKit() {
    }

    public LSDJKit(byte romImage[], int kitIndex) {
        this.romImage = romImage;
        setKitPage(kitIndex);
    }

    public void setRomImage(byte romImage[]) {
        this.romImage = romImage;
    }

    public void setKitPage(int index) {
        pageIndex = index;
        updateSamples();
    }

    private void updateSamples() {
        for (int i = 0; i <16; i++) {
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

    public byte[] get4BitSamples(int index, boolean halfSpeed) {
        int offset = getPageStartAddress() + index * 2;
        int start = (0xff & romImage[offset]) | ((0xff & romImage[offset + 1]) << 8);
        int stop = (0xff & romImage[offset + 2]) | ((0xff & romImage[offset + 3]) << 8);
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

    public void refreshSamples() {

    }
}
