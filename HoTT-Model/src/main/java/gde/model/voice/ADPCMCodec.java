package gde.model.voice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Codec to encode / decode ADPCM (VOX) audio data.
 *
 * <p>
 * see <a href="http://faculty.salina.k-state.edu/tim/vox/dialogic_adpcm.pdf">http://faculty.salina.k-state.edu/tim/vox/dialogic_adpcm.pdf</a> for details.
 * </p>
 *
 * <b>Note:</b> The original implementation deals only with 12-bit PCM linear audio. This version works with 16-bit PCM linear audio.
 *
 * @author oliver.treichel@gmx.de
 */
public class ADPCMCodec {
    private static final int[] QUANTIZER = { -1, -1, -1, -1, 2, 4, 6, 8 };
    private static final int[] STEP_SIZES = { 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190,
            209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552 };
    private static final int MIN_PCM = Short.MIN_VALUE; // -2048 for 12-bit
    private static final int MAX_PCM = Short.MAX_VALUE; // 2047 for 12-bit
    private static final int MIN_INDEX = 0;
    private static final int MAX_INDEX = STEP_SIZES.length - 1;

    /**
     * Decode 4-bit ADPCM encoded data to 16-bit signed PCM (compression ratio 1:4).
     *
     * @param data
     *            ADPCM encoded data. Each byte contains two 4-bit samples.
     * @return The PCM decoded data.
     * @throws IOException
     */
    public static byte[] decode(final byte[] data) throws IOException {
        return IOUtils.toByteArray(decode(new ByteArrayInputStream(data)));
    }

    /**
     * Decode 4-bit ADPCM encoded data to 16-bit signed PCM (compression ratio 1:4).
     *
     * @param source
     *            A stream of ADPCM encoded data. Each byte contains two 4-bit samples.
     * @return A stream of PCM data.
     * @throws IOException
     */
    public static InputStream decode(final InputStream source) throws IOException {
        return new DecodingInputStream(source);
    }

    /**
     * Encode 16-bit signed PCM data to 4-bit ADPCM encoded data (compression ratio 4:1).
     *
     * @param data
     *            The PCM data.
     *
     * @return The ADPCM encoded data. Each byte contains two 4-bit samples.
     * @throws IOException
     */
    public static byte[] encode(final byte[] data) throws IOException {
        return IOUtils.toByteArray(encode(new ByteArrayInputStream(data)));
    }

    /**
     * Encode 16-bit signed PCM data to 4-bit ADPCM encoded data (compression ratio 4:1).
     *
     * @param source
     *            A stream of PCM data.
     * @return A stream of ADPCM encoded data. Each byte contains two 4-bit samples.
     * @throws IOException
     */
    public static InputStream encode(final InputStream source) throws IOException {
        return new EncodingInputStream(source);
    }

    /** index into step size table */
    private int index = 0;

    /** last PCM value */
    private int last = 0;

    /**
     * Decode an ADPCM encoded 4-bit sample to 16-bit signed PCM.
     *
     * @param adpcm
     *            ADPCM encoded sample (4 bit)
     * @return The decoded PCM value (16 bit signed)
     */
    int decode(final int adpcm) {
        // current step size
        final int ss = STEP_SIZES[index];

        // calculate difference
        int diff = ss / 8;
        // magnitude bit 3
        if ((adpcm & 0b0100) != 0) diff += ss;
        // magnitude bit 2
        if ((adpcm & 0b0010) != 0) diff += ss / 2;
        // magnitude bit 1
        if ((adpcm & 0b0001) != 0) diff += ss / 4;
        // sign
        if ((adpcm & 0b1000) != 0) diff = -diff;

        // new PCM value
        last += diff;

        // clipping
        if (last > MAX_PCM) last = MAX_PCM;
        if (last < MIN_PCM) last = MIN_PCM;

        // new step size
        index += QUANTIZER[adpcm & 0b0111];

        // clipping
        if (index < MIN_INDEX) index = MIN_INDEX;
        if (index > MAX_INDEX) index = MAX_INDEX;

        return last;
    }

    /**
     * Encode a 16-bit signed PCM value to 4-bit ADPCM.
     *
     * @param pcm
     *            The PCM value (16 bit signed)
     * @return The ADPCM encoded sample (4 bit)
     */
    int encode(final int pcm) {
        final int ss = STEP_SIZES[index];
        int diff = pcm - last;
        int adpcm = 0;

        // sign
        if (diff < 0) {
            adpcm |= 0b1000;
            diff = -diff;
        }

        // magnitude bit 3
        if (diff > ss) {
            adpcm |= 0b0100;
            diff -= ss;
        }

        // magnitude bit 2
        if (diff > ss / 2) {
            adpcm |= 0b0010;
            diff -= ss / 2;
        }

        // magnitude bit 1
        if (diff > ss / 4) adpcm |= 0b0001;

        last = decode(adpcm);

        return adpcm;
    }

    /**
     * Reset state of the encoder/decoder.
     */
    void reset() {
        index = 0;
        last = 0;
    }
}