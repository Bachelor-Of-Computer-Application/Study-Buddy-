package com.studybuddy.utils;

import javafx.scene.image.Image;
import java.io.FileInputStream;
import java.io.IOException;

public class ImageLoader {

    private static final ImageLoader INSTANCE = new ImageLoader();

    public ImageLoader() {
    }

    public static ImageLoader getInstance() {
        return INSTANCE;
    }

    public Image loadImage(String path) {
        try {
            return new Image(new FileInputStream(path));
        } catch (IOException e) {
            System.out.println("Failed to load image: " + path);
            return getPlaceholderImage();
        }
    }

    public Image loadImageFromResources(String resourceName) {
        String path = "/resources/images/" + resourceName;
        return loadImage(path);
    }

    public Image getPlaceholderImage() {
        // Return a default placeholder image
        return new Image("data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTYwIiBoZWlnaHQ9IjkwIiB4bWxucz0iaHR0cHM6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB2aWV3Qm94PSIwIDAgMTYwIDkwIj48cmVjdCB3aWR0aD0iMTYwIiBoZWlnaHQ9IjkwIiBmaWxsPSIjRjNGNEY2Ii8+PC9zdmc+");
    }

    public Image getUserProfileImage(String username) {
        // Generate or load user profile image
        String path = "/resources/images/profile-" + username.toLowerCase() + ".jpg";
        return loadImage(path);
    }
}