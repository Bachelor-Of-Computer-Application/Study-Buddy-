package com.studybuddy.utils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Circle;
import javax.imageio.ImageIO;

/**
 * Loads, validates, resizes, and displays user profile avatars.
 */
public class ImageLoader {

    private static final Logger logger = Logger.getLogger(ImageLoader.class.getName());
    private static final ImageLoader INSTANCE = new ImageLoader();

    public static final String DEFAULT_AVATAR_RESOURCE = "/com/studybuddy/images/round-account-button.png";
    public static final long MAX_PROFILE_IMAGE_BYTES = 5L * 1024 * 1024;
    public static final int MAX_PROFILE_DIMENSION = 512;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private Image cachedDefaultAvatar;

    private ImageLoader() {}

    public static ImageLoader getInstance() {
        return INSTANCE;
    }

    /** Loads the bundled default avatar (never null). */
    public Image getDefaultAvatarImage() {
        if (cachedDefaultAvatar != null && !cachedDefaultAvatar.isError()) {
            return cachedDefaultAvatar;
        }
        try (InputStream in = ImageLoader.class.getResourceAsStream(DEFAULT_AVATAR_RESOURCE)) {
            if (in != null) {
                cachedDefaultAvatar = new Image(in, 0, 0, true, true);
                if (!cachedDefaultAvatar.isError()) {
                    return cachedDefaultAvatar;
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to load default avatar resource: " + e.getMessage());
        }
        cachedDefaultAvatar = new Image("data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0iI2UyZThmMCIvPjx0ZXh0IHg9IjUwIiB5PSI1NSIgZm9udC1zaXplPSIzMCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iIzk0YTNiOCI+8J+RjTwvdGV4dD48L3N2Zz4=");
        return cachedDefaultAvatar;
    }

    /**
     * Resolves a profile image path to an {@link Image}, falling back to the default avatar.
     */
    public Image loadProfileImage(String profileImagePath) {
        if (profileImagePath == null || profileImagePath.isBlank()) {
            return getDefaultAvatarImage();
        }
        File file = new File(profileImagePath);
        if (!file.exists() || !file.isFile()) {
            logger.warning("Profile image missing: " + profileImagePath);
            return getDefaultAvatarImage();
        }
        try {
            Image image = new Image(file.toURI().toString(), 0, 0, true, true);
            if (image.isError() || image.getWidth() <= 0) {
                logger.warning("Failed to decode profile image: " + profileImagePath);
                return getDefaultAvatarImage();
            }
            return image;
        } catch (Exception e) {
            logger.warning("Error loading profile image: " + e.getMessage());
            return getDefaultAvatarImage();
        }
    }

    /** Applies circular clipping and smooth center-crop scaling to an avatar {@link ImageView}. */
    public void configureCircularAvatar(ImageView imageView, double size) {
        if (imageView == null) return;
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(false);

        Circle clip = imageView.getClip() instanceof Circle existing
                ? existing
                : new Circle(size / 2.0, size / 2.0, size / 2.0);
        clip.setCenterX(size / 2.0);
        clip.setCenterY(size / 2.0);
        clip.setRadius(size / 2.0);
        imageView.setClip(clip);

        Runnable applyCrop = () -> applyCenterCrop(imageView, size);
        if (!Boolean.TRUE.equals(imageView.getProperties().get("avatarConfigured"))) {
            imageView.getProperties().put("avatarConfigured", Boolean.TRUE);
            imageView.imageProperty().addListener((obs, oldImg, newImg) -> applyCrop.run());
        }
        applyCrop.run();
    }

    private void applyCenterCrop(ImageView imageView, double size) {
        Image img = imageView.getImage();
        if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) {
            return;
        }
        double iw = img.getWidth();
        double ih = img.getHeight();
        double side = Math.min(iw, ih);
        double x = (iw - side) / 2.0;
        double y = (ih - side) / 2.0;
        imageView.setViewport(new Rectangle2D(x, y, side, side));
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
    }

    /** Binds an avatar view to a profile path with default fallback. */
    public void applyAvatarToView(ImageView imageView, String profileImagePath, double size) {
        if (imageView == null) return;
        configureCircularAvatar(imageView, size);
        imageView.setImage(loadProfileImage(profileImagePath));
    }

    public void validateProfileImageFile(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IOException("No image file selected.");
        }
        if (file.length() > MAX_PROFILE_IMAGE_BYTES) {
            throw new IOException("Image must be 5 MB or smaller.");
        }
        String ext = extension(file.getName());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IOException("Unsupported format. Use JPG, JPEG, PNG, or WebP.");
        }
        if (!canReadImage(file)) {
            throw new IOException("The selected file is not a valid image.");
        }
    }

    private boolean canReadImage(File file) throws IOException {
        BufferedImage probe = ImageIO.read(file);
        if (probe != null) {
            return true;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            Image fxProbe = new Image(fis, 0, 0, true, true);
            return !fxProbe.isError() && fxProbe.getWidth() > 0;
        }
    }

    /**
     * Validates, optionally resizes, and stores a profile image under ~/StudyBuddy/profiles/.
     *
     * @return absolute path to the saved PNG file
     */
    public String saveProfileImage(File source, int userId) throws IOException {
        validateProfileImageFile(source);
        BufferedImage original = readBufferedImage(source);
        BufferedImage resized = resizeToMaxDimension(original, MAX_PROFILE_DIMENSION);

        Path dir = getProfilesDirectory();
        Files.createDirectories(dir);
        String filename = "user_" + userId + "_" + System.currentTimeMillis() + ".png";
        Path target = dir.resolve(filename);
        ImageIO.write(resized, "png", target.toFile());
        logger.info("Saved profile image: " + target);
        return target.toAbsolutePath().toString();
    }

    public boolean deleteProfileImageFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return true;
        }
        try {
            Path path = Path.of(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
            }
            return true;
        } catch (IOException e) {
            logger.warning("Failed to delete profile image " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    private Path getProfilesDirectory() {
        return Path.of(System.getProperty("user.home"), "StudyBuddy", "profiles");
    }

    private BufferedImage readBufferedImage(File source) throws IOException {
        BufferedImage img = ImageIO.read(source);
        if (img != null) {
            return img;
        }
        try (FileInputStream fis = new FileInputStream(source)) {
            Image fx = new Image(fis, 0, 0, true, true);
            if (fx.isError() || fx.getWidth() <= 0) {
                throw new IOException("Could not read the selected image.");
            }
            return fxImageToBufferedImage(fx);
        }
    }

    private BufferedImage fxImageToBufferedImage(Image fx) {
        int w = (int) Math.max(1, Math.round(fx.getWidth()));
        int h = (int) Math.max(1, Math.round(fx.getHeight()));
        ImageView view = new ImageView(fx);
        view.setPreserveRatio(false);
        view.setFitWidth(w);
        view.setFitHeight(h);
        WritableImage snapshot = new WritableImage(w, h);
        view.snapshot(null, snapshot);
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                buffered.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
            }
        }
        return buffered;
    }

    private static BufferedImage resizeToMaxDimension(BufferedImage src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min(1.0, Math.min((double) maxDim / w, (double) maxDim / h));
        if (scale >= 1.0) {
            return src;
        }
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    public Image loadImage(String path) {
        return loadProfileImage(path);
    }

    public Image getPlaceholderImage() {
        return getDefaultAvatarImage();
    }
}
