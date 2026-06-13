package sk.rigo.photofinish.repository;

import sk.rigo.photofinish.db.Database;
import sk.rigo.photofinish.model.BrandingTemplate;
import sk.rigo.photofinish.model.ImageFitMode;
import sk.rigo.photofinish.model.LogoPosition;
import sk.rigo.photofinish.model.OutputFormat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BrandingTemplateRepository {

  private final Database database;

  public BrandingTemplateRepository(Database database) {
    this.database = database;
  }

  public synchronized BrandingTemplate getOrCreateDefault() throws SQLException {
    List<BrandingTemplate> templates = listAll();
    if (!templates.isEmpty()) {
      return templates.get(0);
    }
    BrandingTemplate template = BrandingTemplate.defaults();
    return save(template);
  }

  public synchronized Optional<BrandingTemplate> findById(long id) throws SQLException {
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM branding_templates WHERE id = ?")) {
      statement.setLong(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(map(resultSet));
        }
      }
    }
    return Optional.empty();
  }

  public synchronized List<BrandingTemplate> listAll() throws SQLException {
    List<BrandingTemplate> templates = new ArrayList<>();
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM branding_templates ORDER BY id");
         ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        templates.add(map(resultSet));
      }
    }
    return templates;
  }

  public synchronized BrandingTemplate save(BrandingTemplate template) throws SQLException {
    if (template.getId() == 0L) {
      return insert(template);
    }
    update(template);
    return template;
  }

  private BrandingTemplate insert(BrandingTemplate template) throws SQLException {
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             INSERT INTO branding_templates (
               name, logo_path, logo_position, logo_scale_percent, logo_opacity, offset_x, offset_y,
               text_bar_enabled, text_template, text_bar_height_percent, text_bar_color, text_color,
               font_name, font_size, output_format,
               canvas_enabled, canvas_width, canvas_height, image_fit_mode, canvas_background_color,
               header_enabled, header_height_percent, header_background_color, header_text_color,
               header_title, header_subtitle, header_left_logo_path, header_right_logo_path,
               results_enabled, results_height_percent, results_title, results_rows_text,
               results_background_color, results_header_color, results_accent_color,
               updated_at
             )
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
             """, Statement.RETURN_GENERATED_KEYS)) {
      bindTemplate(statement, template);
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          template.setId(keys.getLong(1));
        }
      }
      return template;
    }
  }

  private void update(BrandingTemplate template) throws SQLException {
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             UPDATE branding_templates
             SET name = ?, logo_path = ?, logo_position = ?, logo_scale_percent = ?, logo_opacity = ?,
                 offset_x = ?, offset_y = ?, text_bar_enabled = ?, text_template = ?,
                 text_bar_height_percent = ?, text_bar_color = ?, text_color = ?,
                 font_name = ?, font_size = ?, output_format = ?,
                 canvas_enabled = ?, canvas_width = ?, canvas_height = ?, image_fit_mode = ?,
                 canvas_background_color = ?, header_enabled = ?, header_height_percent = ?,
                 header_background_color = ?, header_text_color = ?, header_title = ?, header_subtitle = ?,
                 header_left_logo_path = ?, header_right_logo_path = ?, results_enabled = ?,
                 results_height_percent = ?, results_title = ?, results_rows_text = ?,
                 results_background_color = ?, results_header_color = ?, results_accent_color = ?,
                 updated_at = ?
             WHERE id = ?
             """)) {
      bindTemplate(statement, template);
      statement.setLong(37, template.getId());
      statement.executeUpdate();
    }
  }

  private static void bindTemplate(PreparedStatement statement, BrandingTemplate template) throws SQLException {
    statement.setString(1, template.getName());
    statement.setString(2, template.getLogoPath());
    statement.setString(3, template.getLogoPosition().name());
    statement.setDouble(4, template.getLogoScalePercent());
    statement.setDouble(5, template.getLogoOpacity());
    statement.setInt(6, template.getOffsetX());
    statement.setInt(7, template.getOffsetY());
    statement.setInt(8, template.isTextBarEnabled() ? 1 : 0);
    statement.setString(9, template.getTextTemplate());
    statement.setDouble(10, template.getTextBarHeightPercent());
    statement.setString(11, template.getTextBarColor());
    statement.setString(12, template.getTextColor());
    statement.setString(13, template.getFontName());
    statement.setInt(14, template.getFontSize());
    statement.setString(15, template.getOutputFormat().name());
    statement.setInt(16, template.isCanvasEnabled() ? 1 : 0);
    statement.setInt(17, template.getCanvasWidth());
    statement.setInt(18, template.getCanvasHeight());
    statement.setString(19, template.getImageFitMode().name());
    statement.setString(20, template.getCanvasBackgroundColor());
    statement.setInt(21, template.isHeaderEnabled() ? 1 : 0);
    statement.setDouble(22, template.getHeaderHeightPercent());
    statement.setString(23, template.getHeaderBackgroundColor());
    statement.setString(24, template.getHeaderTextColor());
    statement.setString(25, template.getHeaderTitle());
    statement.setString(26, template.getHeaderSubtitle());
    statement.setString(27, template.getHeaderLeftLogoPath());
    statement.setString(28, template.getHeaderRightLogoPath());
    statement.setInt(29, template.isResultsEnabled() ? 1 : 0);
    statement.setDouble(30, template.getResultsHeightPercent());
    statement.setString(31, template.getResultsTitle());
    statement.setString(32, template.getResultsRowsText());
    statement.setString(33, template.getResultsBackgroundColor());
    statement.setString(34, template.getResultsHeaderColor());
    statement.setString(35, template.getResultsAccentColor());
    statement.setString(36, Instant.now().toString());
  }

  private static BrandingTemplate map(ResultSet resultSet) throws SQLException {
    BrandingTemplate template = new BrandingTemplate();
    template.setId(resultSet.getLong("id"));
    template.setName(resultSet.getString("name"));
    template.setLogoPath(resultSet.getString("logo_path"));
    template.setLogoPosition(LogoPosition.valueOf(resultSet.getString("logo_position")));
    template.setLogoScalePercent(resultSet.getDouble("logo_scale_percent"));
    template.setLogoOpacity(resultSet.getDouble("logo_opacity"));
    template.setOffsetX(resultSet.getInt("offset_x"));
    template.setOffsetY(resultSet.getInt("offset_y"));
    template.setTextBarEnabled(resultSet.getInt("text_bar_enabled") == 1);
    template.setTextTemplate(resultSet.getString("text_template"));
    template.setTextBarHeightPercent(resultSet.getDouble("text_bar_height_percent"));
    template.setTextBarColor(resultSet.getString("text_bar_color"));
    template.setTextColor(resultSet.getString("text_color"));
    template.setFontName(resultSet.getString("font_name"));
    template.setFontSize(resultSet.getInt("font_size"));
    template.setOutputFormat(OutputFormat.valueOf(resultSet.getString("output_format")));
    template.setCanvasEnabled(resultSet.getInt("canvas_enabled") == 1);
    template.setCanvasWidth(resultSet.getInt("canvas_width"));
    template.setCanvasHeight(resultSet.getInt("canvas_height"));
    template.setImageFitMode(ImageFitMode.valueOf(resultSet.getString("image_fit_mode")));
    template.setCanvasBackgroundColor(resultSet.getString("canvas_background_color"));
    template.setHeaderEnabled(resultSet.getInt("header_enabled") == 1);
    template.setHeaderHeightPercent(resultSet.getDouble("header_height_percent"));
    template.setHeaderBackgroundColor(resultSet.getString("header_background_color"));
    template.setHeaderTextColor(resultSet.getString("header_text_color"));
    template.setHeaderTitle(resultSet.getString("header_title"));
    template.setHeaderSubtitle(resultSet.getString("header_subtitle"));
    template.setHeaderLeftLogoPath(resultSet.getString("header_left_logo_path"));
    template.setHeaderRightLogoPath(resultSet.getString("header_right_logo_path"));
    template.setResultsEnabled(resultSet.getInt("results_enabled") == 1);
    template.setResultsHeightPercent(resultSet.getDouble("results_height_percent"));
    template.setResultsTitle(resultSet.getString("results_title"));
    template.setResultsRowsText(resultSet.getString("results_rows_text"));
    template.setResultsBackgroundColor(resultSet.getString("results_background_color"));
    template.setResultsHeaderColor(resultSet.getString("results_header_color"));
    template.setResultsAccentColor(resultSet.getString("results_accent_color"));
    return template;
  }
}
