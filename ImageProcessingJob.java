import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageProcessingJob extends Task<Void> {
    File file;
    File directory;
    DoubleProperty progress;

    public ImageProcessingJob(File file) {
        this.file = file;
        this.directory = null;
        this.progress = new SimpleDoubleProperty();
    }

    @Override
    protected Void call() {
        if (directory == null){
            updateMessage("Null directory");
            return null;
        }
        if (file == null) {
            updateMessage("Null file");
            return null;
        }
        updateMessage("processing...");
        updateProgress(0,1);

        try {
            BufferedImage original = ImageIO.read(file);

            BufferedImage grayscale = new BufferedImage(
                    original.getWidth(), original.getHeight(), original.getType());
            //processing pixel by pixel
            for (int i = 0; i < original.getWidth(); i++) {
                for (int j = 0; j < original.getHeight(); j++) {
                    int red = new Color(original.getRGB(i, j)).getRed();
                    int green = new Color(original.getRGB(i, j)).getGreen();
                    int blue = new Color(original.getRGB(i, j)).getBlue();
                    int luminosity = (int) (0.21*red + 0.71*green + 0.07*blue);
                    int newPixel =
                            new Color(luminosity, luminosity, luminosity).getRGB();
                    grayscale.setRGB(i, j, newPixel);
                }
                double progressC = (1.0 + i) / original.getWidth();
                Platform.runLater(() -> progress.set(progressC));
            }
            Path outputPath =
                    Paths.get(directory.getAbsolutePath(), file.getName());

            ImageIO.write(grayscale, "jpg", outputPath.toFile());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        updateMessage("completed");
        return null;
    }

    public File getFile() {
        return file;
    }

    public DoubleProperty getProgressProperty() {
        return progress;
    }
}
