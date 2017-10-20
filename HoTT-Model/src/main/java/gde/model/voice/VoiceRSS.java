package gde.model.voice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

public class VoiceRSS implements Text2Speech {
    private enum Bits {
        _8BIT, _16BIT
    }

    private enum Channels {
        MONO, STEREO
    }

    private enum Codec {
        MP3, WAV, AAC, OGG, CAF
    }

    private enum Encoding {
        PCM, ALAW, ULAW
    }

    private enum Format {
        pcm_8khz_8bit_mono(Encoding.PCM, SampleRate._8KHZ, Bits._8BIT, Channels.MONO),
        pcm_8khz_8bit_stereo(Encoding.PCM, SampleRate._8KHZ, Bits._8BIT, Channels.STEREO),
        pcm_8khz_16bit_mono(Encoding.PCM, SampleRate._8KHZ, Bits._16BIT, Channels.MONO),
        pcm_8khz_16bit_stereo(Encoding.PCM, SampleRate._8KHZ, Bits._8BIT, Channels.STEREO),
        pcm_11khz_8bit_mono(Encoding.PCM, SampleRate._11KHZ, Bits._8BIT, Channels.MONO),
        pcm_11khz_8bit_stereo(Encoding.PCM, SampleRate._11KHZ, Bits._8BIT, Channels.STEREO),
        pcm_11khz_16bit_mono(Encoding.PCM, SampleRate._11KHZ, Bits._16BIT, Channels.MONO),
        pcm_11khz_16bit_stereo(Encoding.PCM, SampleRate._11KHZ, Bits._16BIT, Channels.STEREO),
        pcm_12khz_8bit_mono(Encoding.PCM, SampleRate._12KHZ, Bits._8BIT, Channels.MONO),
        pcm_12khz_8bit_stereo(Encoding.PCM, SampleRate._12KHZ, Bits._8BIT, Channels.STEREO),
        pcm_12khz_16bit_mono(Encoding.PCM, SampleRate._12KHZ, Bits._16BIT, Channels.MONO),
        pcm_12khz_16bit_stereo(Encoding.PCM, SampleRate._12KHZ, Bits._16BIT, Channels.STEREO),
        pcm_16khz_8bit_mono(Encoding.PCM, SampleRate._16KHZ, Bits._8BIT, Channels.MONO),
        pcm_16khz_8bit_stereo(Encoding.PCM, SampleRate._16KHZ, Bits._8BIT, Channels.STEREO),
        pcm_16khz_16bit_mono(Encoding.PCM, SampleRate._16KHZ, Bits._16BIT, Channels.MONO),
        pcm_16khz_16bit_stereo(Encoding.PCM, SampleRate._16KHZ, Bits._16BIT, Channels.STEREO),
        pcm_22khz_8bit_mono(Encoding.PCM, SampleRate._22KHZ, Bits._8BIT, Channels.MONO),
        pcm_22khz_8bit_stereo(Encoding.PCM, SampleRate._22KHZ, Bits._8BIT, Channels.STEREO),
        pcm_22khz_16bit_mono(Encoding.PCM, SampleRate._22KHZ, Bits._16BIT, Channels.MONO),
        pcm_22khz_16bit_stereo(Encoding.PCM, SampleRate._22KHZ, Bits._16BIT, Channels.STEREO),
        pcm_24khz_8bit_mono(Encoding.PCM, SampleRate._24KHZ, Bits._8BIT, Channels.MONO),
        pcm_24khz_8bit_stereo(Encoding.PCM, SampleRate._24KHZ, Bits._8BIT, Channels.STEREO),
        pcm_24khz_16bit_mono(Encoding.PCM, SampleRate._24KHZ, Bits._16BIT, Channels.MONO),
        pcm_24khz_16bit_stereo(Encoding.PCM, SampleRate._24KHZ, Bits._16BIT, Channels.STEREO),
        pcm_32khz_8bit_mono(Encoding.PCM, SampleRate._32KHZ, Bits._8BIT, Channels.MONO),
        pcm_32khz_8bit_stereo(Encoding.PCM, SampleRate._32KHZ, Bits._8BIT, Channels.STEREO),
        pcm_32khz_16bit_mono(Encoding.PCM, SampleRate._32KHZ, Bits._16BIT, Channels.MONO),
        pcm_32khz_16bit_stereo(Encoding.PCM, SampleRate._32KHZ, Bits._16BIT, Channels.STEREO),
        pcm_44khz_8bit_mono(Encoding.PCM, SampleRate._44KHZ, Bits._8BIT, Channels.MONO),
        pcm_44khz_8bit_stereo(Encoding.PCM, SampleRate._44KHZ, Bits._8BIT, Channels.STEREO),
        pcm_44khz_16bit_mono(Encoding.PCM, SampleRate._44KHZ, Bits._16BIT, Channels.MONO),
        pcm_44khz_16bit_stereo(Encoding.PCM, SampleRate._44KHZ, Bits._16BIT, Channels.STEREO),
        pcm_48khz_8bit_mono(Encoding.PCM, SampleRate._48KHZ, Bits._8BIT, Channels.MONO),
        pcm_48khz_8bit_stereo(Encoding.PCM, SampleRate._48KHZ, Bits._8BIT, Channels.STEREO),
        pcm_48khz_16bit_mono(Encoding.PCM, SampleRate._48KHZ, Bits._16BIT, Channels.MONO),
        pcm_48khz_16bit_stereo(Encoding.PCM, SampleRate._48KHZ, Bits._16BIT, Channels.STEREO), alaw_8khz_mono(Encoding.ALAW, SampleRate._8KHZ, Channels.MONO),
        alaw_8khz_stereo(Encoding.ALAW, SampleRate._8KHZ, Channels.STEREO), alaw_11khz_mono(Encoding.ALAW, SampleRate._11KHZ, Channels.MONO),
        alaw_11khz_stereo(Encoding.ALAW, SampleRate._11KHZ, Channels.STEREO), alaw_22khz_mono(Encoding.ALAW, SampleRate._22KHZ, Channels.MONO),
        alaw_22khz_stereo(Encoding.ALAW, SampleRate._22KHZ, Channels.STEREO), alaw_44khz_mono(Encoding.ALAW, SampleRate._44KHZ, Channels.MONO),
        alaw_44khz_stereo(Encoding.ALAW, SampleRate._44KHZ, Channels.STEREO), ulaw_8khz_mono(Encoding.ALAW, SampleRate._8KHZ, Channels.MONO),
        ulaw_8khz_stereo(Encoding.ALAW, SampleRate._8KHZ, Channels.STEREO), ulaw_11khz_mono(Encoding.ALAW, SampleRate._11KHZ, Channels.MONO),
        ulaw_11khz_stereo(Encoding.ALAW, SampleRate._11KHZ, Channels.STEREO), ulaw_22khz_mono(Encoding.ALAW, SampleRate._22KHZ, Channels.MONO),
        ulaw_22khz_stereo(Encoding.ALAW, SampleRate._22KHZ, Channels.STEREO), ulaw_44khz_mono(Encoding.ALAW, SampleRate._44KHZ, Channels.MONO),
        ulaw_44khz_stereo(Encoding.ALAW, SampleRate._44KHZ, Channels.STEREO);

        private final Encoding encoding;
        private final SampleRate sampleRate;
        private final Bits bits;
        private final Channels channels;

        private Format(final Encoding encoding, final SampleRate sampleRate, final Bits bits, final Channels channels) {
            this.encoding = encoding;
            this.sampleRate = sampleRate;
            this.bits = bits;
            this.channels = channels;
        }

        private Format(final Encoding encoding, final SampleRate sampleRate, final Channels channels) {
            this(encoding, sampleRate, null, channels);
        }

        public String getKey() {
            final StringBuilder sb = new StringBuilder();

            if (encoding == Encoding.PCM)
                sb.append(sampleRate.name().substring(1)).append(bits.name());
            else
                sb.append(encoding.name()).append(sampleRate.name());

            sb.append('-').append(channels.name());

            return sb.toString().toLowerCase();
        }
    }

    private enum SampleRate {
        _8KHZ, _11KHZ, _12KHZ, _16KHZ, _22KHZ, _24KHZ, _32KHZ, _44KHZ, _48KHZ
    }

    private final static String REGEXP = "[^a-zA-Z0-9 _-äöüÄÖÜß]";

    private final static String BASE_ADDRESS = "http://api.voicerss.org/?key=%s&hl=%s&r=%d&c=%s&f=%s&ssml=false&b64=%s&src=%s";
    private final static String VOICE_RSS_DEFAULT_API_KEY = "1def8e9c6ebf4a2eb02fc7b510b04387";
    // private final static String VOICE_RSS_API_KEY = " bb123703dfc1486893ce391ab241ec54"; // oliver.treichel@gmx.de

    private String apiKey = VOICE_RSS_DEFAULT_API_KEY;
    private String text;
    private VoiceRssLanguage language;
    private int speed = 0;
    private Codec codec = Codec.WAV;
    private Format format = Format.pcm_11khz_16bit_mono;
    private boolean base64 = false;

    @Override
    public File convert(final String text, final String language) throws Exception {
        return convert(text, VoiceRssLanguage.forString(language));
    }

    public File convert(final String text, final VoiceRssLanguage language) throws MalformedURLException, UnsupportedEncodingException, IOException {
        setText(text);
        setLanguage(language);
        return getFile();
    }

    public String getApiKey() {
        return apiKey;
    }

    public Codec getCodec() {
        return codec;
    }

    public File getFile() throws MalformedURLException, UnsupportedEncodingException, IOException {
        // Give file a fancy name
        final File tmpdir = new File(System.getProperty("java.io.tmpdir"));

        final String name = text.replaceAll(REGEXP, ""); // remove unsafe characters
        final String filename = name.substring(0, Math.min(17, name.length())); // shorten to 17 characters
        File file = new File(tmpdir, filename + ".wav");

        while (file.exists())
            file = new File(tmpdir, filename + "_" + UUID.randomUUID().toString().substring(0, 3) + ".wav");

        final URLConnection connection = getURL().openConnection();
        try (InputStream is = connection.getInputStream(); OutputStream os = new FileOutputStream(file)) {
            IOUtils.copy(is, os);
        }

        return file;
    }

    public Format getFormat() {
        return format;
    }

    public VoiceRssLanguage getLanguage() {
        return language;
    }

    public int getSpeed() {
        return speed;
    }

    public String getText() {
        return text;
    }

    private URL getURL() throws MalformedURLException, UnsupportedEncodingException {
        return new URL(String.format(BASE_ADDRESS, getApiKey(), getLanguage().getKey(), getSpeed(), getCodec(), getFormat().getKey(),
                Boolean.toString(isBase64()), URLEncoder.encode(getText(), "UTF-8")));
    }

    public boolean isBase64() {
        return base64;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public void setBase64(final boolean base64) {
        this.base64 = base64;
    }

    public void setCodec(final Codec codec) {
        this.codec = codec;
    }

    public void setFormat(final Format format) {
        this.format = format;
    }

    public void setLanguage(final VoiceRssLanguage language) {
        this.language = language;
    }

    public void setSpeed(final int speed) {
        this.speed = speed;
    }

    public void setText(final String text) {
        this.text = text;
    }
}
