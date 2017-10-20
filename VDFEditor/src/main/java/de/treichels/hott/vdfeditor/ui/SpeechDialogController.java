package de.treichels.hott.vdfeditor.ui;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import gde.model.voice.VoiceRSS;
import java.net.UnknownHostException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

class SpeechDialogController {

    private static final ResourceBundle RES = ResourceBundle.getBundle(SpeechDialogController.class.getName());
    private final Stage stage;
    private Text2SpeechTask task = null;
    private Thread thread = null;
    private List<String> languages = null;
    private int selectedLanguage = 0;

    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextArea textArea;
    @FXML
    private ComboBox<String> languageComboBox;

    public SpeechDialogController() {

        try {
            final FXMLLoader loader = new FXMLLoader(getClass().getResource("SpeechDialog.fxml"), RES);
            loader.setController(this);
            final Parent root = loader.load();
            final Scene scene = new Scene(root);

            this.languages = VoiceRSS.getLanguages();
            ObservableList<String> langs =  FXCollections.observableArrayList();
            this.languages.forEach(lang -> langs.add(RES.getString(lang)));
            this.languageComboBox.setItems(langs);
            this.languageComboBox.getSelectionModel().selectFirst();
            this.selectedLanguage = this.languageComboBox.getSelectionModel().selectedIndexProperty().get();

            stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setOnCloseRequest(this::onAbort);
            stage.setScene(scene);

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void closeDialog(final WorkerStateEvent ev) {
        final Throwable t = ev == null ? null : ev.getSource().getException();

        if (t != null) {
            String text;
            if (t instanceof UnknownHostException)
                text = RES.getString("unknown_Host");
            else
                text = t.getLocalizedMessage();

            MessageDialog.show(AlertType.ERROR, RES.getString("error"), text);
        }

        if (thread != null)
            thread.interrupt();

        thread = null;
        progressBar.setProgress(0);
        stage.hide();
    }

    public File getResult() {
        return task.getValue();
    }

    @FXML
    private void onLanguageChanged(final Event ev) {
        this.selectedLanguage = this.languageComboBox.getSelectionModel().selectedIndexProperty().get();
    }

    @FXML
    private void onStart(final Event ev) {
        if (thread != null)
            closeDialog(null);
        else if (!this.textArea.textProperty().getValue().isEmpty()) {
                task.setText(this.textArea.textProperty().getValue());
                task.setLanguage(this.languages.get(this.selectedLanguage));
                progressBar.setProgress(-1.0f);
                thread = new Thread(task);
                thread.start();
        }
    }

    @FXML
    private void onAbort(final Event ev) {
        progressBar.setProgress(0);
        ev.consume();
        stage.close();
        task.cancel();
    }

    public void openDialog(final Text2SpeechTask task) {
        this.task = task;
        stage.titleProperty().set(RES.getString("title"));
        this.textArea.textProperty().set("");

        task.setOnSucceeded(this::closeDialog);
        task.setOnCancelled(this::closeDialog);
        task.setOnFailed(this::closeDialog);

        stage.showAndWait();
    }
}
