package sk.rigo.photofinish.image;

import sk.rigo.photofinish.config.AppSettings;
import sk.rigo.photofinish.model.BrandingTemplate;
import sk.rigo.photofinish.model.OutputFormat;
import sk.rigo.photofinish.model.ProcessedFile;
import sk.rigo.photofinish.model.ProcessingStatus;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

public class ImageExporter {

  private final Path stagingDirectory;

  public ImageExporter(Path stagingDirectory) {
    this.stagingDirectory = stagingDirectory;
  }

  public ExportResult export(BufferedImage image, Path sourcePath, long recordId, AppSettings settings, BrandingTemplate template) throws IOException {
    OutputFormat format = template.getOutputFormat();
    String filename = brandedFileName(sourcePath, recordId, format);
    Path stagedPath = stagingDirectory.resolve(filename);
    writeImage(image, stagedPath, format);

    if (settings.getExportDirectory() == null || settings.getExportDirectory().isBlank()) {
      return new ExportResult(ProcessingStatus.PENDING_EXPORT, null, stagedPath, "Export pending: export directory is not configured");
    }

    Path outputPath = Path.of(settings.getExportDirectory()).resolve(filename);
    try {
      createParentDirectories(outputPath);
      Files.copy(stagedPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
      return new ExportResult(ProcessingStatus.EXPORTED, outputPath, stagedPath, "Exported");
    } catch (IOException ex) {
      return new ExportResult(ProcessingStatus.PENDING_EXPORT, outputPath, stagedPath, "Export pending: " + ex.getMessage());
    }
  }

  public ExportResult retryExport(ProcessedFile file) throws IOException {
    if (file.stagedPath() == null || file.stagedPath().isBlank()) {
      return new ExportResult(ProcessingStatus.FAILED, null, null, "No staged image is available for retry");
    }
    if (file.outputPath() == null || file.outputPath().isBlank()) {
      return new ExportResult(ProcessingStatus.FAILED, null, Path.of(file.stagedPath()), "No output path was stored");
    }

    Path stagedPath = Path.of(file.stagedPath());
    Path outputPath = Path.of(file.outputPath());
    if (!Files.isRegularFile(stagedPath)) {
      return new ExportResult(ProcessingStatus.FAILED, outputPath, stagedPath, "Staged image is missing: " + stagedPath);
    }

    try {
      createParentDirectories(outputPath);
      Files.copy(stagedPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
      return new ExportResult(ProcessingStatus.EXPORTED, outputPath, stagedPath, "Exported on retry");
    } catch (IOException ex) {
      return new ExportResult(ProcessingStatus.PENDING_EXPORT, outputPath, stagedPath, "Still pending: " + ex.getMessage());
    }
  }

  private static void writeImage(BufferedImage image, Path target, OutputFormat format) throws IOException {
    Files.createDirectories(target.getParent());
    if (format == OutputFormat.JPG) {
      writeJpeg(image, target);
    } else {
      ImageIO.write(image, format.imageIoFormat(), target.toFile());
    }
  }

  private static void writeJpeg(BufferedImage image, Path target) throws IOException {
    BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = rgb.createGraphics();
    try {
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
      graphics.drawImage(image, 0, 0, null);
    } finally {
      graphics.dispose();
    }

    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
    if (!writers.hasNext()) {
      throw new IOException("No JPEG writer is available");
    }

    ImageWriter writer = writers.next();
    ImageWriteParam params = writer.getDefaultWriteParam();
    if (params.canWriteCompressed()) {
      params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      params.setCompressionQuality(0.95f);
    }

    try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(target.toFile())) {
      writer.setOutput(outputStream);
      writer.write(null, new IIOImage(rgb, null, null), params);
    } finally {
      writer.dispose();
    }
  }

  private static String brandedFileName(Path sourcePath, long recordId, OutputFormat format) {
    String name = sourcePath.getFileName() == null ? "image" : sourcePath.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String base = dot > 0 ? name.substring(0, dot) : name;
    return sanitize(base) + "_branded_" + recordId + "." + format.extension();
  }

  private static String sanitize(String value) {
    String sanitized = value.replaceAll("[^A-Za-z0-9._-]", "_");
    return sanitized.isBlank() ? "image" : sanitized;
  }

  private static void createParentDirectories(Path path) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }
}
