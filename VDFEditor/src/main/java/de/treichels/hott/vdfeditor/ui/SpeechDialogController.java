package de.treichels.hott.vdfeditor.ui;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import gde.model.voice.VoiceRssLanguage;
import javafx.collections.FXCollections;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

class SpeechDialogController {
    private static final String PREFERRED_LANGUAGE = "preferredLanguage";
    private static final ResourceBundle RES = ResourceBundle.getBundle(SpeechDialogController.class.getName());
    private static final Preferences PREFS = Preferences.userNodeForPackage(SpeechDialogController.class);

    private final Stage stage = new Stage();
    private Text2SpeechTask task = null;
    private Thread thread = null;

    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextArea textArea;
    @FXML
    private ComboBox<VoiceRssLanguage> languageComboBox;
    @FXML
    private Button startButton;
    @FXML
    private Button abortButton;

    public SpeechDialogController() {
        try {
            final FXMLLoader loader = new FXMLLoader(getClass().getResource("SpeechDialog.fxml"), RES);
            loader.setController(this);
            final Parent root = loader.load();

            // get preferred language from prefs
            final VoiceRssLanguage prefLanguage = VoiceRssLanguage.forString(PREFS.get(PREFERRED_LANGUAGE, "de_de"));
            languageComboBox.setItems(FXCollections.observableArrayList(VoiceRssLanguage.values()));
            languageComboBox.getSelectionModel().select(prefLanguage);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setOnCloseRequest(this::onAbort);
            stage.setScene(new Scene(root));
        } catch (final IOException e) {
            ExceptionDialog.show(e);
        }
    }

    public synchronized void closeDialog(final WorkerStateEvent ev) {
        final Throwable t = ev == null ? null : ev.getSource().getException();

        if (t != null) if (t instanceof UnknownHostException)
            MessageDialog.show(AlertType.ERROR, RES.getString("error"), RES.getString("unknown_Host"));
        else
            ExceptionDialog.show(t);

        if (thread != null) thread.interrupt();

        thread = null;
        progressBar.setProgress(0);
        stage.hide();
    }

    private VoiceRssLanguage getLanguage() {
        return languageComboBox.getSelectionModel().getSelectedItem();
    }

    public File getResult() {
        return task.getValue();
    }

    private String getText() {
        return textArea.textProperty().getValue();
    }

    @FXML
    private void onAbort(final Event ev) {
        progressBar.setProgress(0);
        task.cancel();
        ev.consume();
    }

    @FXML
    private void onLanguageChanged() {
        // save selected language as new preferred language
        final VoiceRssLanguage language = getLanguage();
        PREFS.put(PREFERRED_LANGUAGE, language.name());
    }

    @FXML
    private void onStart(final Event ev) {
        task.setText(getText());
        task.setLanguage(getLanguage());
        progressBar.setProgress(-1.0f);
        thread = new Thread(task);
        thread.start();
    }

    public void openDialog(final Text2SpeechTask task) {
        this.task = task;
        stage.titleProperty().set(RES.getString("title"));
        textArea.textProperty().set("");

        task.setOnSucceeded(this::closeDialog);
        task.setOnCancelled(this::closeDialog);
        task.setOnFailed(this::closeDialog);

        // disable start button if text area contains no text or the task is already running
        startButton.disableProperty().bind(textArea.lengthProperty().isEqualTo(0).or(task.runningProperty()));

        // disable abort button if task is not running
        abortButton.disableProperty().bind(task.runningProperty().not());

        textArea.requestFocus();

        stage.showAndWait();
    }
}
