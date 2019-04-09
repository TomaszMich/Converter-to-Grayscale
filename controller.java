import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.*;

public class controller implements Initializable {
    @FXML TableView tableView;
    @FXML TableColumn<ImageProcessingJob, String> imageNameColumn;
    @FXML TableColumn<ImageProcessingJob, Double> progressColumn;
    @FXML TableColumn<ImageProcessingJob, String> statusColumn;
    @FXML Button selectFilesButton;
    @FXML Button chooseDirButton;
    @FXML Button startButton;
    @FXML Button selectThreads;
    @FXML Label infoLabel;
    @FXML AnchorPane pane;
    File selectedDir = null;
    ObservableList<ImageProcessingJob> jobs;
    int threads;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public void chooseDir()
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        selectedDir = directoryChooser.showDialog(null);
        infoLabel.setText("Target directory selected!");
    }

    public void selectFiles()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JPG images", "*.jpg"));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if (selectedFiles == null || selectedFiles.isEmpty())
            return;
        infoLabel.setText("Files selected!");

        jobs.clear();
        for (File selectedFile : selectedFiles) {
            jobs.add(new ImageProcessingJob(selectedFile));
        }
    }

    @FXML void toggleThreads(){
        threads = 0;
        ChoiceBox selectThreads = new ChoiceBox(
                FXCollections.observableArrayList(
                        "Single thread",
                        "Common tread pool",
                        "2 threads",
                        "4 threads",
                        "8 threads"
                )
        );
        selectThreads.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                threads = newValue.intValue();
            }
        });
        selectThreads.getSelectionModel().selectFirst();
        selectThreads.setPrefWidth(150);
        selectThreads.setLayoutX(475.0);
        selectThreads.setLayoutY(4.0);
        pane.getChildren().add(selectThreads);

        threads = selectThreads.getSelectionModel().getSelectedIndex();
    }

    @FXML
    void processFiles() {
        if (selectedDir == null) {
            infoLabel.setText("Select target directory!");
            return;
        }
        if (jobs.isEmpty()) {
            infoLabel.setText("Select files first!");
            return;
        }
        infoLabel.setText("Processing...");
        new Thread(this::backgroundJob).start();
    }

    private void backgroundJob(){
        switch (threads) {
            case 0:
                executor = Executors.newSingleThreadExecutor();
                break;
            case 1:
                executor = new ForkJoinPool();
                break;
            case 2:
                executor = Executors.newFixedThreadPool(2);
                break;
            case 3:
                executor = Executors.newFixedThreadPool(4);
                break;
            case 4:
                executor = Executors.newFixedThreadPool(8);
                break;
        }

        long start = System.currentTimeMillis();

        jobs.stream().forEach(job -> {job.directory = selectedDir;
        executor.submit(job);});
        try {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
            long duration = System.currentTimeMillis() - start;
            Platform.runLater(() -> infoLabel.setText("Finished!\nTotal time: " + ((double)duration)/1000 + "s."));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        imageNameColumn.setCellValueFactory( //file name
                p -> new SimpleStringProperty(p.getValue().getFile().getName()));
        statusColumn.setCellValueFactory( //progress status
                p -> p.getValue().messageProperty());
        progressColumn.setCellFactory( //usage of progress bar
                ProgressBarTableCell.<ImageProcessingJob>forTableColumn());
        progressColumn.setCellValueFactory( //progress
                p -> p.getValue().getProgressProperty().asObject());
        jobs = FXCollections.observableList(new ArrayList<>());
        tableView.setItems(jobs);
    }
}
