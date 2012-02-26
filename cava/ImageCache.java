package cava;

import java.awt.Component;
import java.awt.Image;

import java.util.Hashtable;

/**
 * A Basic Image Cache class.
 */
public class ImageCache {

    private static Hashtable<Thread, Image> images = new Hashtable<Thread, Image>();

    /**
     * Gets an Image of the requested size.
     *
     * Checks if an Image already exists in the cache for the current Thread and
     * if this image is large enough. Else, creates a new Image and store it in
     * the cache.
     */
    static public Image getImage(Component c, int w, int h) {
        Image img = (Image) images.get(Thread.currentThread());
        if ((img == null) || (img.getWidth(c) < w)
                || (img.getHeight(c) < h)) {
            img = c.createImage(w, h);
            images.put(Thread.currentThread(), img);
        }
        return img;
    }
}
