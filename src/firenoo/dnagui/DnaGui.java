package firenoo.dnagui;


import firenoo.dna.Dna;
import firenoo.dna.DnaLoader;
import firenoo.dna.DnaWriter;
import firenoo.dna.IDna;

import firenoo.lib.data.BitUtils;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DnaGui extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Seed initializer");
        Stage start = new Stage();
        new Window(800, 600, start).show();
    }


    private static class Window {

        private final int width, height;
        private final Stage stage;
        private final BorderPane mainLayout;

        private TextField fileTxt;
        private TextField seedTxt;

        private final Scene scene;

        private final SimpleIntegerProperty alleleMode = new SimpleIntegerProperty(DEC);
        private final SimpleIntegerProperty domMode = new SimpleIntegerProperty(BIN);
        private TableView<RiboMode.DnaEntry> display;

        static final int BIN = 0;
        static final int HEX = 1;
        static final int DEC = 2;

        private final DnaLoader dnaLoader;
        private final DnaWriter dnaWriter;
        private IDna dna;

        private RiboMode preset;

        private Window(int width, int height, Stage stage) {
            this.mainLayout = new BorderPane();
            this.width = width;
            this.height = height;
            this.stage = stage;
            this.scene = new Scene(mainLayout);
            this.dnaLoader = new DnaLoader();
            this.dnaWriter = new DnaWriter();
            init();
        }

        public void show() {
            stage.show();
        }

        private void init() {
            stage.setResizable(false);
            stage.setWidth(width);
            stage.setHeight(height);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("FN-DNA Serialization Tool");
            initLayout();
            setPreset(RiboMode.DEFAULT_FIRENOO);
            stage.setScene(scene);
        }


        private void setPreset(RiboMode preset) {
            this.preset = preset;
            final ObservableList<RiboMode.DnaEntry> dnaEntries = FXCollections.observableArrayList();
            dnaEntries.addAll(this.preset.getTraits());
            display.setItems(dnaEntries);
        }

        private void initLayout() {
            VBox editor = new VBox();
            editor.getChildren().addAll(initFileManagement(), initAttributes());
            mainLayout.setTop(initMenu());
            mainLayout.setCenter(editor);
            mainLayout.requestFocus();
            mainLayout.setOnMouseClicked(event -> mainLayout.requestFocus());
        }

        private MenuBar initMenu() {
            final MenuBar menuBar = new MenuBar();
            final Menu options = new Menu("View");
            final Menu alleleViews = new Menu("Allele Data");
            final RadioMenuItem alleleUseBinary = new RadioMenuItem("Binary");
            final RadioMenuItem alleleUseHex = new RadioMenuItem("Hex");
            final RadioMenuItem alleleUseInt = new RadioMenuItem("Decimal");
            ToggleGroup alleleOptions = new ToggleGroup();
            alleleOptions.getToggles().addAll(alleleUseBinary, alleleUseHex, alleleUseInt);
            alleleUseBinary.setOnAction(event -> this.alleleMode.set(BIN));
            alleleUseHex.setOnAction(event -> this.alleleMode.set(HEX));
            alleleUseInt.setOnAction(event -> this.alleleMode.set(DEC));
            alleleViews.getItems().addAll(alleleUseBinary, alleleUseHex, alleleUseInt);
            Menu domViews = new Menu("Dominance Data");
            final RadioMenuItem domUseBinary = new RadioMenuItem("Binary");
            final RadioMenuItem domUseHex = new RadioMenuItem("Hex");
            final RadioMenuItem domUseInt = new RadioMenuItem("Decimal");
            final ToggleGroup domOptions = new ToggleGroup();
            domUseBinary.setOnAction(event -> this.domMode.set(BIN));
            domUseHex.setOnAction(event -> this.domMode.set(HEX));
            domUseInt.setOnAction(event -> this.domMode.set(DEC));
            domOptions.getToggles().addAll(domUseBinary, domUseHex, domUseInt);
            domViews.getItems().addAll(domUseBinary, domUseHex, domUseInt);
            alleleUseInt.setSelected(true);
            domUseBinary.setSelected(true);
            options.getItems().addAll(alleleViews, domViews);
            menuBar.getMenus().addAll(options);
            return menuBar;
        }

        private GridPane initFileManagement() {
            final GridPane fileBox = new GridPane();
            ColumnConstraints cons1 = new ColumnConstraints();
            cons1.setPercentWidth(10);
            cons1.setHalignment(HPos.RIGHT);
            ColumnConstraints cons2 = new ColumnConstraints();
            cons2.setPercentWidth(70);
            ColumnConstraints cons3 = new ColumnConstraints();
            cons3.setPercentWidth(10);
            ColumnConstraints cons4 = new ColumnConstraints();
            cons4.setPercentWidth(10);
            fileBox.getColumnConstraints().addAll(cons1, cons2, cons3, cons4);
            fileBox.setAlignment(Pos.CENTER);
            fileBox.setPadding(new Insets(16, 3, 16, 3));
            fileBox.setHgap(16);
            fileTxt = new TextField(System.getProperty("user.dir"));
            fileTxt.setPrefWidth(600);
            Button loadButton = new Button("Load");
            loadButton.setOnMouseClicked(event -> {
                String path = fileTxt.getText();
                try {
                    File file = new File(path);
                    if(file.isDirectory()) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText("Specified path is a directory. Aborting!");
                        alert.setHeaderText(null);
                        alert.show();
                        return;
                    }
                    try (FileInputStream stream = new FileInputStream(file)){
                        dna = dnaLoader.load(stream);
                    }
                    List<RiboMode.DnaEntry> traits = preset.getTraits();
                    for(int i = 0; i < dna.geneCount(); i++) {
                        int val = BitUtils.byteArrayToInt(dna.getGene(i));
                        if(traits.size() > i) {
                            traits.get(i).setData(val);
                        } else {
                            preset.addTrait("unnamed_" + i, i, val);
                        }
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            });
            loadButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            Button writeButton = new Button("Write");
            writeButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            writeButton.setOnMouseClicked(event -> {
                String path = fileTxt.getText();
                try {
                    File file = new File(path);
                    if(file.isDirectory()) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText("Specified path is a directory. Aborting!");
                        alert.setHeaderText(null);
                        alert.show();
                        return;
                    }
                    if(!file.createNewFile()) {
                        //warning
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Override File");
                        alert.setContentText(String.format("A path with the name %s already exists. " +
                                "Would you like to override the file?", path));
                        alert.setHeaderText(null);
                        ButtonType yesButton = new ButtonType("Yes");
                        ButtonType noButton = new ButtonType("No");
                        alert.getButtonTypes().setAll(yesButton, noButton);
                        Optional<ButtonType> result = alert.showAndWait();
                        if(result.isPresent() && result.get() == noButton) {
                            return;
                        }
                    }
                    long seed = Long.parseLong(seedTxt.getText());
                    this.dna = fromRiboMode(preset, seed);
                    try (FileOutputStream stream = new FileOutputStream(file)){
                        dnaWriter.write(dna, stream);
                    }
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText(null);
                    alert.setContentText("Success.");
                    alert.show();
                } catch(IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText(null);
                    alert.setContentText("An IO error has occurred. Stack trace:");
                    StringWriter strW = new StringWriter();
                    PrintWriter priW = new PrintWriter(strW);
                    e.printStackTrace(priW);
                    TextArea exception = new TextArea(strW.toString());
                    exception.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                    alert.getDialogPane().setExpandableContent(exception);
                    priW.close();
                }
            });
            final Label fileDesc = new Label("File:");
            fileDesc.setTextAlignment(TextAlignment.CENTER);
            fileDesc.setLabelFor(fileTxt);
            fileDesc.setFont(Font.font("Segoe", 16));

            fileBox.add(fileDesc, 0, 0);
            fileBox.add(fileTxt, 1, 0);
            fileBox.add(loadButton, 2, 0);
            fileBox.add(writeButton, 3, 0);
            return fileBox;

        }

        private GridPane initSeedManager() {
            final GridPane seedPane = new GridPane();
            final Label l_seed = new Label("Seed:");
            seedTxt = new TextField("0");
            seedTxt.setTextFormatter(new TextFormatter<>(change -> {
                change.setText(change.getText().replaceAll("\\D", ""));
                return change;
            }));
            l_seed.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            seedTxt.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            ColumnConstraints cons1 = new ColumnConstraints();
            cons1.setPercentWidth(25);
            ColumnConstraints cons2 = new ColumnConstraints();
            cons2.setPercentWidth(75);
            seedPane.add(l_seed, 0, 0);
            seedPane.add(seedTxt, 1, 0);
            seedPane.setHgap(3);
            seedPane.getColumnConstraints().addAll(cons1, cons2);
            return seedPane;
        }

        private GridPane initAttributes() {
            final GridPane attribPane = new GridPane();
            attribPane.setPadding(new Insets(16));
            this.display = new TableView<>();
            display.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            final TableColumn<RiboMode.DnaEntry, Number> posCol = new TableColumn<>("Index");
            posCol.setEditable(false);
            posCol.setSortable(false);
            posCol.setResizable(false);
            posCol.setReorderable(false);
            posCol.prefWidthProperty().bind(display.widthProperty().divide(10));
            posCol.setCellValueFactory(entry -> entry.getValue().getLoc());

            final TableColumn<RiboMode.DnaEntry, String> descCol = new TableColumn<>("Name");
            descCol.setEditable(false);
            descCol.setSortable(false);
            descCol.setResizable(false);
            descCol.setReorderable(false);
            descCol.prefWidthProperty().bind(display.widthProperty().divide(5));
            descCol.setCellValueFactory(entry -> entry.getValue().getName());

            final TableColumn<RiboMode.DnaEntry, String> alleleCol1 = new TableColumn<>("Allele 1");
            alleleCol1.setEditable(false);
            alleleCol1.setSortable(false);
            alleleCol1.setResizable(false);
            alleleCol1.setReorderable(false);
            alleleCol1.prefWidthProperty().bind(display.widthProperty().divide(8));
            alleleCol1.setCellValueFactory(entry ->
                    entry.getValue().getData().of(0xFF, 0, alleleMode)
            );

            final TableColumn<RiboMode.DnaEntry, String> domCol1 = new TableColumn<>("Dom. 1");
            domCol1.setEditable(false);
            domCol1.setSortable(false);
            domCol1.setResizable(false);
            domCol1.setReorderable(false);
            domCol1.prefWidthProperty().bind(display.widthProperty().divide(8));
            domCol1.setCellValueFactory(entry ->
                    entry.getValue().getData().of(0xFF, 8, domMode)
            );

            final TableColumn<RiboMode.DnaEntry, String> alleleCol2 = new TableColumn<>("Allele 2");
            alleleCol2.setEditable(false);
            alleleCol2.setSortable(false);
            alleleCol2.setResizable(false);
            alleleCol2.setReorderable(false);
            alleleCol2.prefWidthProperty().bind(display.widthProperty().divide(8));
            alleleCol2.setCellValueFactory(entry ->
                    entry.getValue().getData().of(0xFF, 16, alleleMode)
            );

            final TableColumn<RiboMode.DnaEntry, String> domCol2 = new TableColumn<>("Dom. 2");
            domCol2.setEditable(false);
            domCol2.setSortable(false);
            domCol2.setResizable(false);
            domCol2.setReorderable(false);
            domCol2.prefWidthProperty().bind(display.widthProperty().divide(8));
            domCol2.setCellValueFactory(entry ->
                    entry.getValue().getData().of(0xFF, 24, domMode)
            );

            final TableColumn<RiboMode.DnaEntry, String> expCol = new TableColumn<>("Expected Value");
            expCol.setEditable(false);
            expCol.setSortable(false);
            expCol.setResizable(false);
            expCol.setReorderable(false);
            expCol.prefWidthProperty().bind(display.widthProperty().divide(6));
            expCol.setCellValueFactory(entry ->
                    entry.getValue().getData().of(data -> {
                        int allele1 = data & 0xFF;
                        int allele2 = (data >>> 16) & 0xFF;
                        int dom1 = (data >>> 8) & 0xFF;
                        int dom2 = (data >>> 24) & 0xFF;
                        int result = partialValue_CB(allele1, dom1, allele2, dom2);
                        switch (alleleMode.get()) {
                            case HEX:
                                return DnaGui.pHexStr(result, 2);
                            case BIN:
                                return DnaGui.pBinaryStr(result, 8);
                            default:
                                return String.valueOf(result);
                        }
                    })
            );

            display.getColumns().addAll(posCol, descCol, alleleCol1, domCol1, alleleCol2, domCol2, expCol);
            final VBox tableBox = new VBox(3);
            final Button bas_editButton = new Button("Editor");
            bas_editButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            final Button editButton = new Button("Direct Edit");
            editButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            ColumnConstraints cons1 = new ColumnConstraints();
            cons1.setPercentWidth(75);
            ColumnConstraints cons2 = new ColumnConstraints();
            cons2.setPercentWidth(25);
            tableBox.setPadding(new Insets(3));
            tableBox.getChildren().addAll(bas_editButton, editButton, initSeedManager());
            attribPane.add(display, 0, 0);
            attribPane.add(tableBox, 1, 0);
            editButton.setDisable(true);
            bas_editButton.setDisable(true);
            display.getSelectionModel().selectedIndexProperty().addListener(obs -> {
                if(((ReadOnlyIntegerProperty) obs).get() == -1) {
                    editButton.setDisable(true);
                    bas_editButton.setDisable(true);
                } else {
                    editButton.setDisable(false);
                    bas_editButton.setDisable(false);
                }
            });
            editButton.setOnMouseClicked(event -> Platform.runLater(() -> {
                DirectEditor window = new DirectEditor(this.stage, display.getSelectionModel().getSelectedItem(), alleleMode.get(), domMode.get());
                window.show();
            }));
            bas_editButton.setOnMouseClicked(event -> Platform.runLater(() -> {
                PresetEditor window = new PresetEditor(this.stage, display.getSelectionModel().getSelectedItem(), alleleMode.get());
                window.show();
            }));
            attribPane.setHgap(6);
            attribPane.getColumnConstraints().addAll(cons1, cons2);
            return attribPane;
        }

        /**
         * Literally copied/pasted from my other project to save time.
         * But optimized to remove some if-statement.
         * Gets the value of the trait from the alleles.
         */
        private int partialValue_CB(int allele1, int dom1, int allele2, int dom2) {
            int result;
            int[] domRanks = DnaGui.domRank(dom1, dom2);
            if(domRanks[0] > domRanks[1]) {
                return allele1 & 0xFF;
            } else if(domRanks[0] < domRanks[1]) {
                return allele2 & 0xFF;
            } else {
                //Removed a switch statement and an ugly if-else block in each case.
                int[] domBits = DnaGui.domType(dom1, dom2);
                double a = Math.round(100.0 / ((domBits[0] ^ domBits[1]) + 1)) / 100.0;
                double[] mult = {1.0 - a * (Math.ceil((domBits[0] ^ domBits[1]) / 4.0)), a};
                int cmp = (domBits[0] - domBits[1]) >>> 31;
                result = (int) ((allele1 * (mult[cmp]) + allele2 * (mult[~cmp & 1])));
                return result;
            }
        }
    }



    private static class Editor {

        final Stage stage;
        final RiboMode.DnaEntry prevEntry;

        private Editor(String title, Stage owner, RiboMode.DnaEntry prevEntry) {
            this.stage = new Stage();
            this.stage.initOwner(owner);
            this.stage.initModality(Modality.WINDOW_MODAL);
            this.stage.setTitle(title);
            this.stage.setResizable(false);
            this.prevEntry = prevEntry;
        }

        void show() {
            stage.show();
        }

        String[] alleleToStr(int mode) {
            String[] result = new String[2];
            switch (mode) {
                case Window.HEX:
                    result[0] = "0x" + pHexStr(prevEntry.getData().get() & 0xFF, 2);
                    result[1] = "0x" + pHexStr((prevEntry.getData().get() >>> 16) & 0xFF, 2);
                    break;
                case Window.DEC:
                    result[0] = String.valueOf(prevEntry.getData().get() & 0xFF);
                    result[1] = String.valueOf((prevEntry.getData().get() >>> 16) & 0xFF);
                    break;
                default:
                    result[0] = "0b" + pBinaryStr(prevEntry.getData().get() & 0xFF, 8);
                    result[1] = "0b" + pBinaryStr((prevEntry.getData().get() >>> 16) & 0xFF, 8);
                    break;
            }
            return result;
        }

        String[] domToStr(int mode) {
            String[] result = new String[2];
            switch (mode) {
                case Window.HEX:
                    result[0] = "0x" + pHexStr((prevEntry.getData().get() >>> 8) & 0xFF, 2);
                    result[1] = "0x" + pHexStr((prevEntry.getData().get() >>> 24) & 0xFF, 2);
                    break;
                case Window.DEC:
                    result[0] = String.valueOf((prevEntry.getData().get() >>> 8) & 0xFF);
                    result[1] = String.valueOf((prevEntry.getData().get() >>> 24) & 0xFF);
                    break;
                default:
                    result[0] = "0b" + pBinaryStr((prevEntry.getData().get() >>> 8) & 0xFF, 8);
                    result[1] = "0b" + pBinaryStr((prevEntry.getData().get() >>> 24) & 0xFF, 8);
                    break;
            }
            return result;
        }

        int parseInput(String text, String id) throws NumberFormatException {
            try {
                if(text.startsWith("0b")) {
                    String s = text.substring(2);
                    //binary mode
                    return Integer.parseInt(s.substring(0, Math.min(s.length(), 8)), 2);
                } else if(text.startsWith("0x")) {
                    String s = text.substring(2).replaceAll("[^0-9a-fA-F]", "");
                    //hex mode
                    return Integer.parseInt(s.substring(0, Math.min(s.length(), 2)), 16);
                } else {
                    return Integer.parseInt(text);
                }
            } catch(NumberFormatException e) {
                NumberFormatException t = new NumberFormatException(String.format("Could not parse input for %s. Details: %n%s", id, e.getMessage()));
                throw t;
            }

        }
    }

    private static final class PresetEditor extends Editor {
        private PresetEditor(Stage owner, RiboMode.DnaEntry prevEntry, int alleleMode) {
            super("Edit Entry", owner, prevEntry);
            this.stage.setMaxWidth(800);
            this.stage.setMaxHeight(600);
            initGraphics(alleleMode);
        }

        private void initGraphics(int alleleMode) {
            final GridPane pane = new GridPane();
            int row = 0;
            pane.setHgap(10);
            pane.setVgap(3);
            pane.setPadding(new Insets(16));
            pane.setAlignment(Pos.CENTER);
            ColumnConstraints name1c = new ColumnConstraints();
            name1c.setMaxWidth(75);
            name1c.setHalignment(HPos.RIGHT);
            ColumnConstraints name2c = new ColumnConstraints();
            name2c.setMaxWidth(150);
            name2c.setHalignment(HPos.LEFT);
            ColumnConstraints name3c = new ColumnConstraints();
            name3c.setMaxWidth(75);
            name3c.setHalignment(HPos.RIGHT);
            ColumnConstraints name4c = new ColumnConstraints();
            name4c.setMaxWidth(150);
            name4c.setHalignment(HPos.LEFT);
            pane.getColumnConstraints().addAll(name1c, name2c, name3c, name4c);
            //Title
            final Label l_title = new Label("Edit DNA Entry");
            l_title.setFont(Font.font("Segoe", 20));
            HBox title = new HBox(l_title);
            title.setAlignment(Pos.BOTTOM_LEFT);
            pane.add(title, 0, row, 4, 1);
            //Name
            final Label l_name = new Label("Name:");
//            l_name.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            final TextField t_name = new TextField(prevEntry.getName().get());
//            t_name.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            pane.add(l_name, 0, ++row);
            pane.add(t_name, 1, row, 3, 1);
            pane.add(new Separator(), 0, ++row, 4, 1);
            //Allele Values
            final Label l_allele = new Label("Allele Values");
            l_allele.setFont(Font.font("Segoe", 16));
            HBox altitle = new HBox(l_allele);
            altitle.setAlignment(Pos.BOTTOM_LEFT);
            pane.add(altitle, 0, ++row, 4, 1);
            String[] alleles = alleleToStr(alleleMode);
            final Label l_al1val = new Label("Allele 1:");
            l_al1val.setAlignment(Pos.CENTER);
            final TextField t_al1val = new TextField(alleles[0]);
            final Label l_al2val = new Label("Allele 2:");
            l_al2val.setAlignment(Pos.CENTER);
            final TextField t_al2val = new TextField(alleles[1]);
            t_al1val.setId("allele1");
            t_al2val.setId("allele2");
            pane.addRow(++row, l_al1val, t_al1val, l_al2val, t_al2val);
            final Button swapAlValues = new Button("Swap Values");
            swapAlValues.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            swapAlValues.setOnMouseClicked(event -> {
                String s = t_al2val.getText();
                t_al2val.setText(t_al1val.getText());
                t_al1val.setText(s);
            });
            pane.add(swapAlValues, 0, ++row, 2, 1);
            pane.add(new Separator(), 0, ++row, 4, 1);
            //Dominance Ranks
            final Label l_domRank = new Label("Allele Dominance Ranks");
            l_domRank.setFont(new Font("Segoe", 16));
            final HBox domRank = new HBox(l_domRank);
            domRank.setAlignment(Pos.BOTTOM_LEFT);
            pane.add(domRank, 0, ++row, 4, 1);
            String[] doms = domRank();
            final Label l_domR1 = new Label("Allele 1:");
            final Label l_domR2 = new Label("Allele 2:");
            final TextField t_domR1 = new TextField(doms[0]);
            final TextField t_domR2 = new TextField(doms[1]);
            t_domR1.setTextFormatter(new TextFormatter<>(this::updateDR));
            t_domR2.setTextFormatter(new TextFormatter<>(this::updateDR));
            pane.addRow(++row, l_domR1, t_domR1, l_domR2, t_domR2);
            final Button swapDomRank = new Button("Swap Ranks");
            swapDomRank.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            swapDomRank.setOnMouseClicked(event -> {
                String s = t_domR2.getText();
                t_domR2.setText(t_domR1.getText());
                t_domR1.setText(s);
            });
            pane.add(swapDomRank, 0, ++row, 2, 1);

            pane.add(new Separator(), 0, ++row, 4, 1);
            //High Allele
            final Label l_highA = new Label("High Allele");
            l_highA.setFont(Font.font("Segoe", 16));
            HBox highA = new HBox(l_highA);
            highA.setAlignment(Pos.BOTTOM_LEFT);
            final RadioButton[] highAllele = new RadioButton[] {
                    new RadioButton("Allele 1"),
                    new RadioButton("Allele2")
            };
            highAllele[0].setId("h_0");
            highAllele[1].setId("h_1");
            ToggleGroup highs = new ToggleGroup();
            highs.getToggles().addAll(highAllele);
            pane.add(highA, 0, ++row, 4, 1);
            pane.add(new HBox(highAllele[0]), 0, ++row, 2, 1);
            pane.add(new HBox(highAllele[1]), 2, row, 2, 1);
            int[] domBits = domType();
            int type = domBits[0] ^ domBits[1];
            //If-statement free!
            highAllele[((domBits[0] - domBits[1]) >>> 31) & 1].setSelected(true);
            pane.add(new Separator(), 0, ++row, 4, 1);
            //Dominance Behavior
            final Label l_equiv = new Label("Equivalence Behavior");
            l_equiv.setFont(Font.font("Segoe", 16));
            HBox equiv = new HBox(l_equiv);
            equiv.setAlignment(Pos.BOTTOM_LEFT);
            pane.add(equiv, 0, ++row, 4, 1);
            final RadioButton[] domButtons = new RadioButton[] {
                    new RadioButton("Codominance"),
                    new RadioButton("Partial 50/50"),
                    new RadioButton("Partial 67/33"),
                    new RadioButton("Partial 75/25"),
            };
            final ToggleGroup tg1 = new ToggleGroup();
            domButtons[0].setId("d_0");
            domButtons[1].setId("d_1");
            domButtons[2].setId("d_3");
            domButtons[3].setId("d_2");
            tg1.getToggles().addAll(domButtons);
            //Selects the appropriate buttons
            domButtons[type].setSelected(true);
            pane.add(new HBox(domButtons[0]), 0, ++row, 4, 1);
            pane.add(new HBox(domButtons[1]), 0, ++row, 4, 1);
            pane.add(new HBox(domButtons[2]), 0, ++row, 4, 1);
            pane.add(new HBox(domButtons[3]), 0, ++row, 4, 1);
            final Button confirm = new Button("Confirm");
            confirm.setDefaultButton(true);
            confirm.setOnMouseClicked(event -> {
                int result = prevEntry.getData().get();
                result &= ~0xFFF0FFF; //clear dominance bits and allele bits
                AtomicReference<String> currentId = new AtomicReference<>(t_al1val.getId());
                try{
                    int allele1 = parseInput(t_al1val.getText(), t_al1val.getId());
                    currentId.set(t_al2val.getId());
                    int allele2 = parseInput(t_al2val.getText(), t_al2val.getId());
                    result |= allele1 & 0xFF;
                    result |= (allele2 & 0xFF) << 16;
                    result |= (parseInput(t_domR1.getText(), "dom1") & 0b11) << 8;
                    result |= (parseInput(t_domR2.getText(), "dom2") & 0b11) << 24;
                    int i = Integer.parseInt(((RadioButton)tg1.getSelectedToggle()).getId().substring(2));
                    int j = Integer.parseInt(((RadioButton)highs.getSelectedToggle()).getId().substring(2));
                    //'Tis a magical piston tape. If-statements, BEGONE!
                    result |=  (208670528 << (i + (j * 4))) & 0xC000C00;
                    prevEntry.setData(result);
                } catch(NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    for(Node node : pane.getChildren()) {
                        if(currentId.get().equals(node.getId())) {
                            node.requestFocus();
                            break;
                        }
                    }
                    alert.setHeaderText(null);
                    alert.setContentText(e.getMessage());
                    alert.show();
                }
                prevEntry.setName(t_name.getText());
                stage.close();
            });
            final Button cancel = new Button("Cancel");
            cancel.setCancelButton(true);
            cancel.setOnMouseClicked(event -> stage.close());
            cancel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            confirm.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            pane.add(confirm, 0, ++row, 2, 1);
            pane.add(cancel, 2, row, 2, 1);
            stage.setScene(new Scene(pane));
            pane.requestFocus();
        }

        private String[] domRank() {
            String[] result = new String[2];
            result[0] = String.valueOf((prevEntry.getData().get() >>> 8) & 0b11);
            result[1] = String.valueOf((prevEntry.getData().get() >>> 24) & 0b11);
            return result;
        }

        private int[] domType() {
            int[] result = new int[2];
            result[0] = (prevEntry.getData().get() >>> 10) & 0b11;
            result[1] = (prevEntry.getData().get() >>> 26) & 0b11;
            return result;
        }

        TextFormatter.Change updateDR(TextFormatter.Change change) {
            if(change.getControlText().length() == 1 && !change.isReplaced()) {
                change.setText("");
                return change;
            }
            String s = change.getText().replaceAll("[\\D-]", "");
            if(!s.isEmpty() && Integer.parseInt(s) > 3) {
                s = "3";
            }
            change.setText(s);
            return change;
        }
    }

    private static final class DirectEditor extends Editor {

        private DirectEditor(Stage owner, RiboMode.DnaEntry prevEntry, int alleleMode, int domMode) {
            super("Edit Entry", owner, prevEntry);
            initGraphics(alleleMode, domMode);
        }

        public void initGraphics(int alleleMode, int domMode) {
            final GridPane pane = new GridPane();
            int row = 0;
            pane.setPadding(new Insets(16));
            pane.setVgap(3);
            pane.setHgap(3);
            pane.setAlignment(Pos.CENTER);
            ColumnConstraints name1c = new ColumnConstraints();
            name1c.setMaxWidth(75);
            name1c.setHalignment(HPos.RIGHT);
            ColumnConstraints name2c = new ColumnConstraints();
            name2c.setMaxWidth(150);
            name2c.setHalignment(HPos.LEFT);
            ColumnConstraints name3c = new ColumnConstraints();
            name3c.setMaxWidth(75);
            name3c.setHalignment(HPos.RIGHT);
            ColumnConstraints name4c = new ColumnConstraints();
            name4c.setMaxWidth(150);
            name4c.setHalignment(HPos.LEFT);
            pane.getColumnConstraints().addAll(name1c, name2c, name3c, name4c);

            final Label l_title = new Label("Direct Entry Editor");
            l_title.setFont(Font.font("Segoe", 20));
            pane.add(new HBox(l_title), 0, row, 4, 1);
            pane.add(new Separator(), 0, ++row, 4, 1);
            final Label l_name = new Label("Name:");
            final TextField t_name = new TextField(prevEntry.getName().get());
            pane.add(l_name, 0, ++row);
            pane.add(t_name, 1, row, 3, 1);
            String[] alleles = alleleToStr(alleleMode);
            String[] doms = domToStr(domMode);

            final Label l_allele1 = new Label("Allele 1:");
            final Label l_allele2 = new Label("Allele 2:");
            final Label l_dom1 = new Label("Dominance 1:");
            final Label l_dom2 = new Label("Dominance 2:");
            final TextField t_allele1 = new TextField(alleles[0]);
            final TextField t_allele2 = new TextField(alleles[1]);
            final TextField t_dom1 = new TextField(doms[0]);
            final TextField t_dom2 = new TextField(doms[1]);
            final Label l_altitle = new Label("Allele Value");
            l_altitle.setFont(Font.font("Segoe", 16));
            pane.add(new HBox(l_altitle), 0, ++row, 4, 1);
            t_allele1.setId("allele1");
            t_allele2.setId("allele2");
            t_dom1.setId("dom1");
            t_dom2.setId("dom2");
            pane.addRow(++row, l_allele1, t_allele1, l_allele2, t_allele2);
            final Label l_domtitle = new Label("Dominance Value");
            l_domtitle.setFont(Font.font("Segoe", 16));
            pane.add(new HBox(l_domtitle), 0, ++row, 4, 1);
            pane.addRow(++row, l_dom1, t_dom1, l_dom2, t_dom2);
            Button confirmButton = new Button("Confirm");
            confirmButton.setDefaultButton(true);
            confirmButton.setOnAction(event -> {
                AtomicReference<String> currentId = new AtomicReference<>(t_allele1.getId());
                try {
                    int al1 = parseInput(t_allele1.getText(), t_allele1.getId());
                    currentId.set(t_allele2.getId());
                    int al2 = parseInput(t_allele2.getText(), t_allele2.getId());
                    currentId.set(t_dom1.getId());
                    int d1 = parseInput(t_dom1.getText(), "dom1");
                    currentId.set(t_dom2.getId());
                    int d2 = parseInput(t_dom2.getText(), "dom2");
                    prevEntry.setData((al1) | (d1 << 8) | (al2 << 16) | (d2 << 24));
                    stage.close();
                } catch(NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    for(Node node : pane.getChildren()) {
                        if(currentId.get().equals(node.getId())) {
                            node.requestFocus();
                            break;
                        }
                    }
                    alert.setHeaderText(null);
                    alert.setContentText(e.getMessage());
                    alert.show();
                }
            });
            Button cancelButton = new Button("Cancel");
            cancelButton.setCancelButton(true);
            cancelButton.setOnAction(event -> stage.close());
            confirmButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            cancelButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            pane.add(confirmButton, 0, ++row, 2, 1);
            pane.add(cancelButton, 2, row, 2, 1);
            this.stage.setScene(new Scene(pane));
            pane.requestFocus();

        }


    }

    public static String pBinaryStr(int val, int maxCap) {
        StringBuilder b = new StringBuilder(maxCap);
        b.append(Integer.toBinaryString(val));
        while(b.length() < maxCap) {
            b.insert(0, '0');
        }
        return b.toString();
    }

    public static String pHexStr(int val, int maxCap) {
        StringBuilder b = new StringBuilder(maxCap);
        b.append(Integer.toHexString(val));
        while(b.length() < maxCap) {
            b.insert(0, '0');
        }
        return b.toString();

    }

    public static int[] domType(int dom1, int dom2) {
        int[] result = new int[2];
        result[0] = (dom1 >>> 2) & 0b11;
        result[1] = (dom2 >>> 2) & 0b11;
        return result;
    }

    public static int[] domRank(int dom1, int dom2) {
        int[] result = new int[2];
        result[0] = dom1 & 0b11;
        result[1] = dom2 & 0b11;
        return result;
    }

    public static IDna fromRiboMode(RiboMode mode, long seed) {
        Dna dna = new Dna(mode.getTraits().size() * 4, seed);
        dna.append(new int[mode.getTraits().size()]);
        for(RiboMode.DnaEntry entry : mode.getTraits()) {
            dna.set(entry.getLoc().get() * 4, BitUtils.intToByteArray(entry.getData().get()));
        }
        return dna;
    }

}

