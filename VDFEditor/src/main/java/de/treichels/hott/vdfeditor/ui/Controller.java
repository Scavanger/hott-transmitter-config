package de.treichels.hott.vdfeditor.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.PrintException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.itextpdf.text.DocumentException;
import com.sun.javafx.collections.ObservableListWrapper;

import de.treichels.hott.HoTTDecoder;
import de.treichels.hott.HoTTSerialPort;
import de.treichels.hott.vdfeditor.actions.InsertAction;
import de.treichels.hott.vdfeditor.actions.MoveAction;
import de.treichels.hott.vdfeditor.actions.MoveDownAction;
import de.treichels.hott.vdfeditor.actions.MoveUpAction;
import de.treichels.hott.vdfeditor.actions.RemoveAction;
import de.treichels.hott.vdfeditor.actions.ReplaceAction;
import de.treichels.hott.vdfeditor.actions.UndoBuffer;
import gde.mdl.messages.Messages;
import gde.model.HoTTException;
import gde.model.enums.TransmitterType;
import gde.model.voice.CountryCode;
import gde.model.voice.VDFType;
import gde.model.voice.VoiceData;
import gde.model.voice.VoiceFile;
import gde.report.ReportException;
import gde.report.html.HTMLReport;
import gde.report.pdf.PDFReport;
import gde.util.Util;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;

@SuppressWarnings("restriction")
public class Controller {
    private static final DataFormat DnD_DATA_FORMAT = new DataFormat(VoiceDataListCell.class.getName());
    private static final String USER_HOME = "user.home"; //$NON-NLS-1$
    private static final String _WAV = "*.wav"; //$NON-NLS-1$
    private static final String _MP3 = "*.mp3"; //$NON-NLS-1$
    private static final String _OGG = "*.ogg"; //$NON-NLS-1$
    private static final String _PDF = "*.pdf"; //$NON-NLS-1$
    private static final String _VDF = "*.vdf"; //$NON-NLS-1$
    private static final String WAV = ".wav"; //$NON-NLS-1$
    private static final String MP3 = ".mp3"; //$NON-NLS-1$
    private static final String OGG = ".ogg"; //$NON-NLS-1$
    private static final String PDF = ".pdf"; //$NON-NLS-1$
    private static final String VDF = ".vdf"; //$NON-NLS-1$
    private static final String LAST_LOAD_VDF_DIR = "lastLoadVdfDir"; //$NON-NLS-1$
    private static final String LAST_SAVE_VDF_DIR = "lastSaveVdfDir"; //$NON-NLS-1$
    private static final String LAST_LOAD_SOUND_DIR = "lastLoadSoundDir"; //$NON-NLS-1$
    private static final Preferences PREFS = Preferences.userNodeForPackage(Controller.class);
    private static final ResourceBundle RES = ResourceBundle.getBundle(Controller.class.getName());
    private static final List<File> TEMP_FILE_LIST = new ArrayList<>();
    private static final Pattern NAME_PATTERN = Pattern.compile("^[0-9]+([._])(.*)$");

    static final Image ICON = new Image(Controller.class.getResource("icon.png").toString());

    /**
     * Delete temporary file that were created during drag&drop.
     *
     * @param ev
     */
    private static void deleteTempFiles(final DragEvent ev) {
        TEMP_FILE_LIST.stream().forEach(f -> f.delete());
        TEMP_FILE_LIST.clear();
    }

    /**
     * Check if the file is a supported sound file.
     *
     * @param file
     * @return
     */
    public static boolean isSoundFormat(final File file) {
        final String name = file.getName();
        return name.endsWith(WAV) | name.endsWith(MP3) | name.endsWith(OGG);
    }

    /**
     * Check if the file is a VDF.
     *
     * @param file
     * @return
     */
    public static boolean isVDF(final File file) {
        return file.getName().endsWith(VDF);
    }

    @FXML
    private ContextMenu contextMenu;
    @FXML
    private MenuItem contextMenuDelete;
    @FXML
    private MenuItem deleteSoundMenuItem;
    @FXML
    private Menu editMenu;
    @FXML
    private Menu addMenu;
    @FXML
    private Menu replaceMenu;
    @FXML
    private ListView<VoiceData> listView;
    @FXML
    private MenuItem moveDownMenuItem;
    @FXML
    private MenuItem moveUpMenuItem;
    @FXML
    private MenuItem playMenuItem;
    @FXML
    private MenuItem renameMenuItem;
    @FXML
    private MenuItem saveVDFMenuItem;
    @FXML
    private MenuItem addSoundMenuItem;
    @FXML
    private MenuItem addSoundTextMenuItem;
    @FXML
    private ComboBox<TransmitterType> transmitterTypeCombo;
    @FXML
    private ComboBox<CountryCode> countryCodeCombo;
    @FXML
    private MenuItem saveVDFAsMenuItem;
    @FXML
    private MenuItem replaceSoundMenuItem;
    @FXML
    private MenuItem replaceSoundFromTextMenuItem;
    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;
    @FXML
    private MenuItem playOnTransmitter;
    @FXML
    private MenuItem writeToTransmitter;
    @FXML
    private ComboBox<Float> vdfVersionCombo;
    @FXML
    private MenuItem loadUserVoiceFiles;
    @FXML
    private Menu restoreMenu;

    final VoiceFile voiceFile = new VoiceFile(VDFType.User, TransmitterType.mc28, 3000, 0);
    private final BooleanBinding systemVDFBinding = voiceFile.vdfTypeProperty().isEqualTo(VDFType.System);
    private final BooleanBinding dirtyProperty = voiceFile.dirtyProperty();
    private final SimpleObjectProperty<File> vdfFileProperty = new SimpleObjectProperty<>(null);
    final UndoBuffer<VoiceData> undoBuffer = new UndoBuffer<>();
    private final ObjectProperty<HoTTSerialPort> serialPortProperty = new SimpleObjectProperty<>();
    private final DialogController dialogController = new DialogController(serialPortProperty);
    private final SpeechDialogController s2tController = new SpeechDialogController();

    private boolean mute = false;

    private void addRestoreFiles() {
        final Map<String, Menu> menus = new TreeMap<>();

        for (final TransmitterType t : TransmitterType.values()) {
            if (t == TransmitterType.unknown) continue;

            final Menu menu = new Menu(t.toString());
            final Map<String, MenuItem> items = new TreeMap<>();

            addRestoreFiles(items, t, null);
            addRestoreFiles(items, t, "Microcopter");
            addRestoreFiles(items, t, "Team");

            menu.getItems().addAll(items.values());
            menus.put(t.toString(), menu);
        }

        restoreMenu.getItems().addAll(menus.values());
    }

    private void addRestoreFiles(final Map<String, MenuItem> items, final TransmitterType t, final String variant) {
        final String location = t == TransmitterType.mc26 || t == TransmitterType.mc28 ? "Voice3_mc28" : "Voice3";

        for (final Language l : Language.values()) {
            final String path = variant == null ? String.format("restore/%s_%s.vdf", location, l.name())
                    : String.format("restore/%s_%s_%s.vdf", location, l.name(), variant);

            final URL url = getClass().getResource(path);
            if (url != null) {
                final String text = variant == null ? l.toString() : String.format("%s (%s)", l.toString(), variant);
                final MenuItem menuItem = new MenuItem(text);
                menuItem.setOnAction(ev -> {
                    try (InputStream is = getClass().getResourceAsStream(path)) {
                        if (askSave()) {
                            final VoiceFile vf = HoTTDecoder.decodeVDF(IOUtils.toByteArray(is));
                            vf.setTransmitterType(t);
                            open(vf);
                        }
                    } catch (final IOException e) {
                        ExceptionDialog.show(e);
                    }
                });
                items.put(text, menuItem);
            }
        }
    }

    public boolean askSave() {
        // no need to save
        if (!dirtyProperty.get()) return true;

        // show waring dialog
        final ButtonType discardButton = new ButtonType(RES.getString("discard_button"));
        final ButtonType saveButton = new ButtonType(RES.getString("save_button"));
        final Alert alert = new Alert(AlertType.WARNING, RES.getString("save_changes"), saveButton, discardButton, ButtonType.CANCEL); //$NON-NLS-1$
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ICON);
        alert.setHeaderText(RES.getString("modified")); //$NON-NLS-1$
        final Optional<ButtonType> answer = alert.showAndWait();

        // yes, discard changes
        if (answer.get() == discardButton) {
            voiceFile.clean();
            return true;
        }

        // cancel, do nothing
        if (!answer.isPresent() || answer.get() == ButtonType.CANCEL) return false;

        // call save dialog and load new file only if save succeeds
        return vdfFileProperty.get() == null ? onSaveAs() : onSave();
    }

    /**
     * sanity check for system voice files to prevent transmitter malfunction
     *
     * @return
     */
    private boolean checkSize() {
        try {
            HoTTDecoder.verityVDF(voiceFile);

            // display a disclaimer when saving a system vdf
            if (systemVDFBinding.get()) {
                final ButtonType accept = new ButtonType(RES.getString("accept_button"));
                final Alert alert = new Alert(AlertType.WARNING, RES.getString("system_vdf_disclaimer_body"), accept, ButtonType.CANCEL);
                ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ICON);
                alert.setHeaderText(RES.getString("system_vdf_disclaimer_title"));
                final Optional<ButtonType> result = alert.showAndWait();
                return result.isPresent() && result.get() == accept;
            }
        } catch (final HoTTException e) {
            ExceptionDialog.show(e);
            return false;
        }

        return true;
    }

    /**
     * Factory method to create new {@link VoiceDataListCell} objects.
     *
     * @param listView
     * @return
     */
    private VoiceDataListCell createListCell(final ListView<VoiceData> listView) {
        final VoiceDataListCell cell = new VoiceDataListCell(this);

        // setup DnD
        cell.setOnDragDetected(this::onDragDetected);
        cell.setOnDragOver(ev -> onDragOver(ev, cell));
        cell.setOnDragEntered(this::onDragEntered);
        cell.setOnDragExited(this::onDragExited);
        cell.setOnDragDropped(this::onDragDropped);
        cell.setOnDragDone(Controller::deleteTempFiles);

        return cell;
    }

    @FXML
    public void initialize() throws IOException {
        // set factory method
        listView.setCellFactory(this::createListCell);

        // enable multi-select
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // clear selection if clicked on an empty row
        listView.setOnMouseClicked(ev -> listView.getSelectionModel().clearSelection());

        // clear selection if ESC key was pressed
        listView.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) listView.getSelectionModel().clearSelection();
        });

        // accept a drop into empty area of listview
        listView.setOnDragOver(ev -> onDragOver(ev, listView));

        // perform drop actions on listview
        listView.setOnDragDropped(this::onDragDropped);

        // setup combo boxes
        countryCodeCombo.getItems().addAll(CountryCode.values());
        transmitterTypeCombo.getItems().addAll(TransmitterType.values());
        transmitterTypeCombo.getItems().remove(TransmitterType.unknown);

        // disable items if no vdf was loaded
        // final BooleanBinding empty = voiceFile.voiceCountProperty().isEqualTo(0);
        // vdfVersionCombo.disableProperty().bind(empty);
        // countryCodeCombo.disableProperty().bind(empty);
        // transmitterTypeCombo.disableProperty().bind(empty);
        // saveVDFMenuItem.disableProperty().bind(empty.or(vdfFileProperty.isNull()).or(dirtyProperty.not()).or(systemVDFBinding));
        saveVDFMenuItem.disableProperty().bind(vdfFileProperty.isNull().or(dirtyProperty.not()));
        // saveVDFAsMenuItem.disableProperty().bind(empty);
        addMenu.disableProperty().bind(systemVDFBinding);
        // writeToTransmitter.disableProperty().bind(empty);

        // no user voice files for vdf version 2.0
        // loadUserVoiceFiles.disableProperty().bind(vdfVersionCombo.valueProperty().isEqualTo(2.0f));

        // disable menu items id no row was selected
        final BooleanBinding noSelection = listView.getSelectionModel().selectedItemProperty().isNull();

        contextMenuDelete.disableProperty().bind(noSelection);
        moveDownMenuItem.disableProperty().bind(noSelection.or(systemVDFBinding));
        moveUpMenuItem.disableProperty().bind(noSelection.or(systemVDFBinding));
        playMenuItem.disableProperty().bind(noSelection);
        playOnTransmitter.disableProperty().bind(noSelection.or(serialPortProperty.isNull()));
        renameMenuItem.disableProperty().bind(noSelection);
        deleteSoundMenuItem.disableProperty().bind(noSelection);
        replaceMenu.disableProperty().bind(noSelection);

        addRestoreFiles();

        undoMenuItem.disableProperty().bind(undoBuffer.canUndo().not());
        redoMenuItem.disableProperty().bind(undoBuffer.canRedo().not());

        // always start with an empty vdf
        onNew();

        updateCheck(false);
    }

    /**
     * Show about dialog.
     */
    @FXML
    public void onAbout() {
        new MessageDialog(AlertType.INFORMATION, Launcher.getTitle(), true, RES.getString("about_text")).showAndWait();
    }

    /**
     * Show file selector to add a new sound file.
     * <p>
     * The method will remember the directory being used.
     */
    @FXML
    public void onAddSound() {
        final List<File> files = selectSound(true);
        addSound(files);
    }

    /**
     * Show a dialog to create a announcement from text, using a online speech-to-text service (VoiceRSS)
     */
    @FXML
    public void onAddSoundFromText() {
        s2tController.openDialog(new Text2SpeechTask());
        final File file = s2tController.getResult();
        if (file != null) {
            addSound(file);
            TEMP_FILE_LIST.add(file);
        }
    }

    /**
     * Exit the application.
     */
    @FXML
    public void onClose() {
        if (askSave()) System.exit(0);
    }

    @FXML
    public void onCountryCodeChanged() {
        final CountryCode oldCountry = voiceFile.getCountry();

        try {
            voiceFile.setCountry(countryCodeCombo.getValue());
            verify();
        } catch (final HoTTException e) {
            if (e.getCause() != null) ExceptionDialog.show(e);
            voiceFile.setCountry(oldCountry);
            Platform.runLater(() -> countryCodeCombo.setValue(oldCountry));
        }
    }

    /**
     * Delete the selected sounds.
     */
    @FXML
    public void onDeleteSound() {
        final MultipleSelectionModel<VoiceData> selectionModel = listView.getSelectionModel();
        final ObservableList<Integer> selectedIndices = selectionModel.getSelectedIndices();

        if (systemVDFBinding.get())
            // keep number of items constant in system VDFs by replacing selected item(s) with an empty place holder
            selectedIndices.stream()
                    .forEach(i -> undoBuffer.push(new ReplaceAction<>(i, new VoiceData(String.format("%02d.%s", i + 1, RES.getString("empty")), null))));
        else
            // sort indices in reverse order and remove from high to low
            selectedIndices.stream().sorted((i, j) -> j - i).forEach(i -> undoBuffer.push(new RemoveAction<>(i)));

        // fix selection - select the item after the last deleted one
        final int index = selectionModel.getSelectedIndex() + 1;
        final int maxIndex = listView.getItems().size() - 1;
        selectionModel.clearSelection();
        selectionModel.select(Math.min(maxIndex, index));
    }

    /**
     * Handle DnD start event.
     * <p>
     * Three drop targets are supported:
     * <ul>
     * <li><b>Drop on desktop</b>: Create temporary .wav files for any selected item and add the path to the clipboard.</li>
     * <li><b>Another VDFEditor instance</b>: Serialize any selected item into the clipboard.</li>
     * <li><b>other (e.g. text editor)</b>: Add the name of any selected item as comma separated list to the clibboard.</li>
     * </ul>
     *
     * @param ev
     */
    private void onDragDetected(final MouseEvent ev) {
        deleteTempFiles(null);

        final ClipboardContent content = new ClipboardContent();

        // selected items
        final ObservableList<VoiceData> selectedItems = listView.getSelectionModel().getSelectedItems();

        // DnD to desktop: generate temporary .wav files for each selected item for DnD to desktop (export of .wav file)
        selectedItems.stream().filter(vd -> vd.getRawData().length > 0).forEach(vd -> {
            try {
                final File wav = new File(System.getProperty("java.io.tmpdir"), vd.getName().replaceAll("[^a-zA-Z0-9.-_]", "_") + Controller.WAV);
                vd.writeWav(wav);
                TEMP_FILE_LIST.add(wav);
            } catch (final IOException e) {
                ExceptionDialog.show(e);
            }
        });
        content.putFiles(TEMP_FILE_LIST);

        // VDFEditor: serialize selected items for DnD to other VDFEditor instance
        content.put(DnD_DATA_FORMAT, new ArrayList<>(selectedItems));

        // other: item name for DnD to editor or text field
        content.putString(StringUtils.join(selectedItems.stream().map(vd -> vd.getName()).toArray(), ","));

        final Dragboard db = ((Node) ev.getSource()).startDragAndDrop(TransferMode.COPY_OR_MOVE);
        db.setContent(content);

        ev.consume();
    }

    /**
     * Handle Dnd drop event.
     * <p>
     * There are three use cases:
     * <ul>
     * <li><b>DnD within the same list</b>: Copy (with CTRL) or move items within the list.</li>
     * <li><b>DnD between VDFEditor instances</b>: Deserialize items from the clipboard.</li>
     * <li><b>DnD from desktop</b>: import any sound or vdf files.</li>
     * </ul>
     *
     * @param ev
     */
    private void onDragDropped(final DragEvent ev) {
        try {
            final Dragboard dragboard = ev.getDragboard();
            final Object source = ev.getGestureSource();
            final Object target = ev.getGestureTarget();
            final int targetIndex = target instanceof VoiceDataListCell ? ((VoiceDataListCell) target).getIndex() : listView.getItems().size();
            final boolean isSystemVDF = systemVDFBinding.get();

            if (source instanceof VoiceDataListCell && target instanceof VoiceDataListCell && source != target) {
                // DnD within the same list

                if (!isSystemVDF) {
                    // only for user VDFs
                    final int sourceIndex = ((VoiceDataListCell) source).getIndex();

                    undoBuffer.push(new MoveAction<>(sourceIndex, targetIndex));
                    listView.getSelectionModel().clearSelection();
                }
            } else if (source == null)
                // DnD from external source

                if (dragboard.hasContent(DnD_DATA_FORMAT)) {
                    // external source is another VDFEditor

                    @SuppressWarnings("unchecked")
                    final ArrayList<VoiceData> list = (ArrayList<VoiceData>) dragboard.getContent(DnD_DATA_FORMAT);

                    if (isSystemVDF)
                        // replace items beginning at target index with list items
                        for (int i = 0; i < list.size(); i++)
                            undoBuffer.push(new ReplaceAction<>(targetIndex + i, list.get(i)));
                    else
                        // insert all items at target index
                        list.forEach(item -> undoBuffer.push(new InsertAction<>(targetIndex, item)));

                } else if (ev.getDragboard().hasFiles()) {
                    // DnD from desktop
                    final List<File> files = dragboard.getFiles();

                    // import first .vdf file
                    if (files.stream().allMatch(Controller::isVDF)) open(files.get(0));

                    // import any sound files
                    if (files.stream().allMatch(Controller::isSoundFormat)) if (isSystemVDF)
                        // replace items beginning at target index with files
                        for (int i = 0; i < files.size(); i++)
                            undoBuffer.push(new ReplaceAction<>(targetIndex + i, VoiceData.readSoundFile(files.get(i))));
                    else
                        files.stream().map(VoiceData::readSoundFile).forEach(item -> undoBuffer.push(new InsertAction<>(targetIndex, item)));
                }
        } catch (final RuntimeException e) {
            ExceptionDialog.show(e);
        }

        ev.setDropCompleted(true);
        ev.consume();
    }

    /**
     * Handle DnD event when the mouse pointer enters a node.
     *
     * @param ev
     */
    private void onDragEntered(final DragEvent ev) {
        if ((!systemVDFBinding.get() || ev.getGestureSource() == null) && !ev.getTarget().equals(ev.getGestureSource()))
            ((Node) ev.getTarget()).setOpacity(0.3d);
        ev.consume();
    }

    /**
     * Handle DnD event when the mouse pointer leaves a node.
     *
     * @param ev
     */
    private void onDragExited(final DragEvent ev) {
        ((Node) ev.getTarget()).setOpacity(1.0d);
        ev.consume();
    }

    /**
     * Report supported transfer modes for the node under the mouse pointer.
     *
     * @param ev
     *            The event.
     * @param target
     *            The node that triggered the event. Note, that we can't use {@link DragEvent#getGestureTarget()} here, because the drag is still in progress
     *            and {@link DragEvent#getGestureTarget()} would return null. We also can't use {@link DragEvent#getTarget()}, as this could be a cild of the
     *            node triggering the event (e.g. an internal label).
     */
    private void onDragOver(final DragEvent ev, final Node target) {
        final Dragboard dragboard = ev.getDragboard();
        final int itemCount = listView.getItems().size();
        final Object source = ev.getGestureSource();
        final int targetIndex = target instanceof VoiceDataListCell ? ((VoiceDataListCell) target).getIndex() : itemCount;
        final boolean isSystemVDF = systemVDFBinding.get();

        if (source instanceof VoiceDataListCell) {
            // DnD within the same list

            if (!isSystemVDF && target instanceof VoiceDataListCell && source != target)
                // allow only for user VDFs and if source != target
                ev.acceptTransferModes(TransferMode.MOVE);

        } else if (source == null)
            // DnD from external source

            if (dragboard.hasContent(DnD_DATA_FORMAT)) {
                // external source is another VDFEditor

                if (isSystemVDF) {
                    // allow multiple items to replace an existing items for system VDFs if they fit in the list
                    @SuppressWarnings("unchecked")
                    final ArrayList<VoiceData> list = (ArrayList<VoiceData>) dragboard.getContent(DnD_DATA_FORMAT);
                    if (targetIndex + list.size() <= itemCount) ev.acceptTransferModes(TransferMode.COPY);
                } else
                    // allow multiple items for user VDFs
                    ev.acceptTransferModes(TransferMode.COPY);

            } else if (dragboard.hasFiles()) {
                // DnD from desktop
                final List<File> files = dragboard.getFiles();

                if (files.stream().allMatch(Controller::isVDF) && files.size() == 1)
                    // only one VDF file was dragged
                    ev.acceptTransferModes(TransferMode.COPY);
                else if (files.stream().allMatch(Controller::isSoundFormat))
                    // only sound files were dragged
                    if (isSystemVDF) {
                        if (targetIndex + files.size() <= itemCount)
                            // allow only multiple sound files to replace existing sounds for system VDFs it they fit in the existing list
                            ev.acceptTransferModes(TransferMode.COPY);
                    } else
                        // allow multiple sound files for user VDFs
                        ev.acceptTransferModes(TransferMode.COPY);
            }

        ev.consume();
    }

    @FXML
    public void onLoadSystemVoiceFile() throws HoTTException {
        if (askSave()) {
            dialogController.openDialog(new LoadVoiceFileTask(RES.getString("load_system_voicefiles"), false));
            final VoiceFile result = dialogController.getResult();
            if (result != null) open(result);
        }
    }

    @FXML
    public void onLoadUserVoiceFile() throws HoTTException {
        if (askSave()) {
            dialogController.openDialog(new LoadVoiceFileTask(RES.getString("load_user_voicefiles"), true));
            final VoiceFile result = dialogController.getResult();
            if (result != null) open(result);
        }
    }

    /**
     * Move the selected item down in the list.
     */
    @FXML
    public void onMoveDown() {
        if (!systemVDFBinding.get()) {
            final int selectedIndex = listView.getSelectionModel().getSelectedIndex();

            undoBuffer.push(new MoveDownAction<>(selectedIndex));
            listView.getSelectionModel().clearSelection();
            listView.getSelectionModel().select(selectedIndex + 1);
        }
    }

    /**
     * Move the selected item up in the list.
     */
    @FXML
    public void onMoveUp() {
        if (!systemVDFBinding.get()) {
            final int selectedIndex = listView.getSelectionModel().getSelectedIndex();

            undoBuffer.push(new MoveUpAction<>(selectedIndex));
            listView.getSelectionModel().clearSelection();
            listView.getSelectionModel().select(selectedIndex - 1);
        }
    }

    /**
     * Create a new empty VDF. Ask to save the old VDF if it was modified.
     *
     * @throws HoTTException
     */
    @FXML
    public void onNew() throws HoTTException {
        if (askSave()) {
            voiceFile.getVoiceData().clear();
            voiceFile.setVdfType(VDFType.User);
            voiceFile.setTransmitterType(TransmitterType.mc28);
            voiceFile.setVdfVersion(3000);
            vdfFileProperty.set(null);
            open(voiceFile);
        }
    }

    /**
     * Show file selector to open a VDF.
     * <p>
     * The method will remember the directory being used.
     *
     * @throws IOException
     */
    @FXML
    public void onOpen() throws IOException {
        if (askSave()) {
            final FileChooser chooser = new FileChooser();
            chooser.setTitle(RES.getString("open_vdf")); //$NON-NLS-1$

            // use dir from preferences
            final File dir = new File(PREFS.get(LAST_LOAD_VDF_DIR, System.getProperty(USER_HOME)));
            if (dir.exists() && dir.isDirectory()) chooser.setInitialDirectory(dir);

            // setup file name filter
            chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("vdf_files"), _VDF)); //$NON-NLS-1$

            final File vdf = chooser.showOpenDialog(listView.getScene().getWindow());
            if (vdf != null) {
                // store dir in preferences
                PREFS.put(LAST_LOAD_VDF_DIR, vdf.getParentFile().getAbsolutePath());
                open(vdf);
            }
        }
    }

    /**
     * Playback the sound of the selected item.
     */
    @FXML
    public void onPlay() {
        listView.getSelectionModel().getSelectedItems().forEach(VoiceData::play);
    }

    @FXML
    public void onPlayOnTransmitter() throws IOException {
        final int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        int offset = 0;

        if (!systemVDFBinding.get()) {
            // temporarily switch to system vdf
            voiceFile.setVdfType(VDFType.System);
            offset = HoTTDecoder.getMaxVoiceCount(voiceFile);
            voiceFile.setVdfType(VDFType.User);
        }

        try (HoTTSerialPort port = serialPortProperty.get()) {
            port.open();
            port.playSound(selectedIndex + offset);
        }
    }

    @FXML
    public void onPrint() throws ReportException, IOException, DocumentException, PrintException {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(RES.getString("print")); //$NON-NLS-1$

        // use dir from preferences
        final File dir = new File(PREFS.get(LAST_SAVE_VDF_DIR, System.getProperty(USER_HOME)));
        if (dir.exists() && dir.isDirectory()) chooser.setInitialDirectory(dir);

        // setup file name filter
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("pdf_files"), _PDF)); //$NON-NLS-1$

        // preset report name
        if (vdfFileProperty.get() != null) {
            final String fileName = vdfFileProperty.get().getName().replaceAll(VDF, PDF);
            chooser.setInitialFileName(fileName);
        }

        final File pdf = chooser.showSaveDialog(listView.getScene().getWindow());
        if (pdf != null) {
            final String name = vdfFileProperty.isNull().get() ? "" : vdfFileProperty.get().getName();
            final String title = String.format("%s VDF V%s", voiceFile.getVdfType(), Float.toString(voiceFile.getVdfVersion() / 1000.0f));
            final String version = Launcher.getTitle();
            final String html = HTMLReport.generateHTML(name, title, version, voiceFile);
            PDFReport.save(pdf, html);
            Desktop.getDesktop().open(pdf);
        }
    }

    @FXML
    public void onRedo() {
        undoBuffer.redo();
        listView.refresh();
        setTitle();
    }

    /**
     * Rename the selected item.
     */
    @FXML
    public void onRename() {
        listView.edit(listView.getSelectionModel().getSelectedIndex());
    }

    /**
     * Replace the selected item.
     */
    @FXML
    public void onReplaceSound() {
        replaceSound(selectSound(false).get(0));
    }

    /**
     * Replace the selected item with a generated sound file
     */
    @FXML
    public void onReplaceSoundFromText() {
        s2tController.openDialog(new Text2SpeechTask());
        final File file = s2tController.getResult();
        if (file != null) {
            replaceSound(file);
            TEMP_FILE_LIST.add(file);
        }
    }

    /**
     * Show dialog to save the current VDF.
     * <p>
     * This method will remember the directory being used.
     *
     * @return
     */
    @FXML
    public boolean onSave() {
        if (!checkSize() || !dirtyProperty.get()) return false;
        return save(vdfFileProperty.get());
    }

    /**
     * Show dialog to save the current VDF under a new name.
     * <p>
     * This method will remember the directory being used.
     *
     * @return
     */
    @FXML
    public boolean onSaveAs() {
        if (!checkSize()) return false;

        final FileChooser chooser = new FileChooser();
        chooser.setTitle(RES.getString("save_vdf")); //$NON-NLS-1$

        // use dir from preferences
        final File dir = new File(PREFS.get(LAST_SAVE_VDF_DIR, System.getProperty(USER_HOME)));
        if (dir.exists() && dir.isDirectory()) chooser.setInitialDirectory(dir);

        // setup file name filter
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("vdf_files"), _VDF)); //$NON-NLS-1$

        final File vdf = chooser.showSaveDialog(listView.getScene().getWindow());
        return save(vdf);
    }

    /**
     * Handle event for change in transmitter type.
     *
     * @param ev
     */
    @FXML
    public void onTransmitterTypeChanged() {
        final TransmitterType newTransmitterType = transmitterTypeCombo.getValue();
        if (newTransmitterType == null) return;

        final TransmitterType oldTtransmitterType = voiceFile.getTransmitterType();
        final int oldVdfVersion = voiceFile.getVdfVersion();

        if (oldTtransmitterType != newTransmitterType) try {
            voiceFile.setTransmitterType(newTransmitterType);
            updateVdfVersion();
            verify();
            setTitle();
        } catch (final Exception e) {
            if (e.getCause() != null) ExceptionDialog.show(e);
            voiceFile.setTransmitterType(oldTtransmitterType);
            voiceFile.setVdfVersion(oldVdfVersion);
            Platform.runLater(() -> transmitterTypeCombo.setValue(oldTtransmitterType));
        }
    }

    @FXML
    public void onUndo() {
        undoBuffer.undo();
        listView.refresh();
        setTitle();
    }

    @FXML
    public void onUpdateCheck() {
        updateCheck(true);
    }

    @FXML
    public void onUserManual() throws IOException {
        // try translated manual first
        InputStream is = getClass().getResourceAsStream(String.format("VDFEditor_%s.pdf", System.getProperty("user.language").toUpperCase()));

        // fall-back
        if (is == null) is = getClass().getResourceAsStream("VDFEditor_EN.pdf");

        if (is != null) {
            final File tempFile = new File(System.getProperty("java.io.tmpdir"), "VDFEditor_Manual.pdf");
            tempFile.deleteOnExit();
            try (OutputStream os = new FileOutputStream(tempFile)) {
                IOUtils.copy(is, os);
            }
            Desktop.getDesktop().open(tempFile);
        }
    }

    @FXML
    public void onVdfVersionChanged() {
        final Float vdfVersion = vdfVersionCombo.getValue();
        if (vdfVersion == null) return;

        final int oldVdfVersion = voiceFile.getVdfVersion();
        final int newVdfVersion = (int) (vdfVersion * 1000);

        if (oldVdfVersion != newVdfVersion) try {
            voiceFile.setVdfVersion(newVdfVersion);
            updateVdfVersion();
            verify();
            setTitle();
        } catch (final HoTTException e) {
            if (e.getCause() != null) ExceptionDialog.show(e);
            voiceFile.setVdfVersion(oldVdfVersion);
            Platform.runLater(() -> updateVdfVersion());
        }
    }

    @FXML
    public void onWriteToTransmitter() {
        dialogController.openDialog(new SendVoiceFileTask(RES.getString("write_to_transmitter"), voiceFile));
    }

    /**
     * Load a new VDF from file. Ask for save if current VDF was modified.
     *
     * @param vdf
     */
    public void open(final File vdf) {
        if (askSave()) try {
            vdfFileProperty.set(vdf);
            open(HoTTDecoder.decodeVDF(vdf));
        } catch (final IOException e) {
            ExceptionDialog.show(e);
        }
    }

    /**
     * Load a new VDF from data model.
     *
     * @param voiceFile
     * @throws HoTTException
     */
    private void open(final VoiceFile other) throws HoTTException {
        voiceFile.copy(other);

        transmitterTypeCombo.setValue(voiceFile.getTransmitterType());
        countryCodeCombo.setValue(voiceFile.getCountry());
        updateVdfVersion();

        final ObservableList<VoiceData> items = new ObservableListWrapper<>(voiceFile.getVoiceData());
        // add a change listener to the list to prevent invalid VDFs
        items.addListener((ListChangeListener<VoiceData>) c -> {
            while (!mute && c.next())
                // if an item was added, check if the size limit was reached
                if (c.wasAdded()) {
                    final int fistIndex = c.getFrom();
                    int lastIndex = c.getTo();

                    // check for duplicate entries
                    for (int i = fistIndex; i < lastIndex && i < items.size(); i++) {
                        final VoiceData voiceData1 = items.get(i);
                        final String name1 = voiceData1.getName();
                        // final int hash1 = Arrays.hashCode(voiceData1.getRawData());

                        for (int j = 0; j < items.size(); j++) {
                            if (j == i) continue;

                            final VoiceData voiceData2 = items.get(j);
                            final String name2 = voiceData2.getName();
                            // final int hash2 = Arrays.hashCode(voiceData2.getRawData());

                            if (name1.equals(name2)) {
                                undoBuffer.pop();
                                lastIndex--;
                                MessageDialog.show(AlertType.ERROR, RES.getString("duplicate_entry"), RES.getString("duplicate_name"), name1);
                                break;
                            }

                            // if (hash1 != 1 && hash1 == hash2) {
                            // undoBuffer.pop();
                            // lastIndex--;
                            // MessageDialog.show(AlertType.ERROR, RES.getString("duplicate_entry"), RES.getString("duplicate_sound"), name1, name2);
                            // break;
                            // }
                        }

                        // enforce name for system VDFs
                        if (systemVDFBinding.get()) {
                            final Matcher matcher = NAME_PATTERN.matcher(name1);
                            final String newName = matcher.matches() ? String.format("%02d%s%s", i + 1, matcher.group(1), matcher.group(2))
                                    : String.format("%02d.%s", i + 1, name1);
                            voiceData1.setName(newName);
                        }
                    }

                    // remove added entries until validation succeeds
                    while (true)
                        try {
                            // validate VDF
                            HoTTDecoder.verityVDF(voiceFile);

                            // validation succeeded - no exception was thrown
                            break;
                        } catch (final HoTTException e) {
                            // validation failed, remove last added item and try again
                            undoBuffer.pop();
                            lastIndex--;
                            ExceptionDialog.show(e);
                        }
                }

            setTitle();
        });

        listView.setItems(items);
        undoBuffer.setItems(items);
        verify(false);
        voiceFile.clean();
        setTitle();
    }

    private boolean save(final File vdf) {
        if (vdf != null) {
            // store dir in preferences
            PREFS.put(LAST_SAVE_VDF_DIR, vdf.getParentFile().getAbsolutePath());

            try {
                HoTTDecoder.encodeVDF(voiceFile, vdf);
                vdfFileProperty.set(vdf);
                voiceFile.clean();
                setTitle();

                // everything ok
                return true;
            } catch (final IOException e) {
                ExceptionDialog.show(e);
            }
        }

        // something went wrong
        return false;
    }

    private List<File> selectSound(final boolean multiSelect) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(RES.getString("load_sound")); //$NON-NLS-1$

        // use dir from preferences
        final File dir = new File(PREFS.get(LAST_LOAD_SOUND_DIR, System.getProperty(USER_HOME)));
        if (dir != null && dir.exists() && dir.isDirectory()) chooser.setInitialDirectory(dir);

        // setup file name filter
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("sound_files"), _WAV, _MP3, _OGG)); //$NON-NLS-1$
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("wav_files"), _WAV)); //$NON-NLS-1$
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("mp3_files"), _MP3)); //$NON-NLS-1$
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("ogg_files"), _OGG)); //$NON-NLS-1$

        final Window window = listView.getScene().getWindow();

        return multiSelect ? chooser.showOpenMultipleDialog(window) : Collections.singletonList(chooser.showOpenDialog(window));
    }

    /**
     * Set stage title with information about the current VDF.
     */
    void setTitle() {
        final StringBuilder sb = new StringBuilder();

        if (dirtyProperty.get()) sb.append("*"); //$NON-NLS-1$

        if (vdfFileProperty.get() == null)
            sb.append(RES.getString("empty")); //$NON-NLS-1$
        else
            sb.append(vdfFileProperty.get().getName());

        try {
            final int maxDataSize = HoTTDecoder.getMaxDataSize(voiceFile);
            final int dataSize = voiceFile.getDataSize();

            sb.append(String.format(" - %d kb / %d kb (%d%%) - %s VDF V%s", dataSize / 1024, maxDataSize / 1024, dataSize * 100 / maxDataSize, //$NON-NLS-1$
                    voiceFile.getVdfType(), Float.toString(voiceFile.getVdfVersion() / 1000.0f)));
        } catch (final HoTTException e) {
            ExceptionDialog.show(e);
        }

        final Scene scene = listView.getScene();
        if (scene != null) ((Stage) scene.getWindow()).setTitle(sb.toString());
    }

    private void updateCheck(final boolean verbose) {
        final String currentVersion = Launcher.getVersion();
        final String latestVersion = Util.getLatestVersion("VDFEditor");

        if (currentVersion.equals(Messages.getString("Launcher.Unknown"))) {
            if (verbose) MessageDialog.show(AlertType.WARNING, RES.getString("unknown_version"), RES.getString("unknown_version_text"));
        } else if (latestVersion == null) {
            if (verbose) MessageDialog.show(AlertType.WARNING, RES.getString("offline"), RES.getString("offline_text"));
        } else if (latestVersion.equals(currentVersion)) {
            if (verbose) MessageDialog.show(AlertType.INFORMATION, RES.getString("uptodate"), RES.getString("uptodate_text"));
        } else
            MessageDialog.show(AlertType.INFORMATION, RES.getString("update_available"), true, RES.getString("update_available_text"), latestVersion);
    }

    private void updateVdfVersion() {
        final ObservableList<Float> items = vdfVersionCombo.getItems();
        final TransmitterType transmitterType = voiceFile.getTransmitterType();
        final int version;

        if (transmitterType == TransmitterType.mc26 || transmitterType == TransmitterType.mc28) {
            version = 3000;
            items.remove(2.0f);
            items.remove(2.5f);
            if (!items.contains(3.0f)) items.add(3.0f);
        } else if (transmitterType == TransmitterType.mz12 || transmitterType == TransmitterType.mz18 || transmitterType == TransmitterType.mz24
                || transmitterType == TransmitterType.x8e || transmitterType == TransmitterType.x8n) {
            version = 2000;
            if (!items.contains(2.0f)) items.add(2.0f);
            items.remove(2.5f);
            items.remove(3.0f);
        } else if (transmitterType == TransmitterType.mz12Pro || transmitterType == TransmitterType.mz24Pro) {
            version = 2500;
            items.remove(2.0f);
            if (!items.contains(2.5f)) items.add(2.5f);
            items.remove(3.0f);
        } else if (voiceFile.getVdfType() == VDFType.User) {
            version = 2500;
            items.remove(2.0f);
            if (!items.contains(2.5f)) items.add(2.5f);
            items.remove(3.0f);
        } else {
            version = voiceFile.getVdfVersion() == 3000 ? 2500 : voiceFile.getVdfVersion();
            if (!items.contains(2.0f)) items.add(2.0f);
            if (!items.contains(2.5f)) items.add(2.5f);
            items.remove(3.0f);
        }

        voiceFile.setVdfVersion(version);
        vdfVersionCombo.setValue(voiceFile.getVdfVersion() / 1000f);
    }

    private void verify() throws HoTTException {
        verify(true);
    }

    private void verify(final boolean showDialog) throws HoTTException {
        final int maxVoiceCount = HoTTDecoder.getMaxVoiceCount(voiceFile);
        int voiceCount = voiceFile.getVoiceCount();
        final ObservableList<VoiceData> voiceData = listView.getItems();

        // remove extra items
        if (voiceCount > maxVoiceCount && showDialog && new MessageDialog(AlertType.CONFIRMATION, RES.getString("delete_entries_header"),
                RES.getString("delete_entries_message"), voiceCount - maxVoiceCount).showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            throw new HoTTException(null);

        mute = true;
        while (voiceCount-- > maxVoiceCount)
            voiceData.remove(voiceCount);

        // add missing items
        if (systemVDFBinding.get()) while (++voiceCount < maxVoiceCount)
            voiceData.add(new VoiceData(String.format("%02d.%s", voiceCount + 1, RES.getString("empty")), null));
        mute = false;

        HoTTDecoder.verityVDF(voiceFile);
    }

    private void replaceSound(File file) {
        if (file != null && file.exists()) {
            final int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            undoBuffer.push(new ReplaceAction<>(selectedIndex, VoiceData.readSoundFile(file)));
        }
    }

    private void addSound(File file) {
        if (file != null)
            addSound(Arrays.asList(file));
    }

    private void addSound(List<File> files) {
        if (files != null) {
            // store dir in preferences
            files.stream().filter(Controller::isSoundFormat).findFirst().map(File::getParentFile).map(File::getAbsolutePath)
                    .ifPresent(s -> PREFS.put(LAST_LOAD_SOUND_DIR, s));

            final int selectedIndex = listView.getSelectionModel().getSelectedIndex();

            // add to end of the list if no item was selected
            final int index = selectedIndex == -1 ? listView.getItems().size() : selectedIndex;

            // insert all sound files at index
            files.stream().filter(Controller::isSoundFormat).map(VoiceData::readSoundFile).forEach(item -> undoBuffer.push(new InsertAction<>(index, item)));
        }
    }
}