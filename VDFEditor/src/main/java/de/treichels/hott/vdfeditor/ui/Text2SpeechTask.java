package de.treichels.hott.vdfeditor.ui;

import java.io.File;

import gde.model.voice.VoiceRSS;
import gde.model.voice.VoiceRssLanguage;
import javafx.concurrent.Task;

final class Text2SpeechTask extends Task<File> {
    private final VoiceRSS voiceRSS = new VoiceRSS();

    @Override
    protected File call() throws Exception {
        return voiceRSS.getFile();
    }

    public VoiceRssLanguage getLanguage() {
        return voiceRSS.getLanguage();
    }

    public String getText() {
        return voiceRSS.getText();
    }

    public void setLanguage(final VoiceRssLanguage language) {
        voiceRSS.setLanguage(language);
    }

    public void setText(final String text) {
        voiceRSS.setText(text);
    }
}
