package a2;

import java.io.*;
import java.nio.*;

import com.jogamp.opengl.*;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.Graphics2D;

/**
 * Created by Connor on 10/21/2015.
 * Based on code from prog 6_1
 */

public class TextureReader {
    public TextureReader() {
    }

    public int loadTexture(GLAutoDrawable drawable, String textureFileName) {
        GL gl = drawable.getGL();

        BufferedImage textureImage = getBufferedImage(textureFileName);
        byte[] imgRGBA = getRGBAPixelData(textureImage);
        ByteBuffer wrappedRGBA = ByteBuffer.wrap(imgRGBA);

        int[] textureIDs = new int[1];
        gl.glGenTextures(1, textureIDs, 0);
        int textureID = textureIDs[0];

        // make the textureID the "current texture"
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureID);

        // attach the image texture to the currently active OpenGL texture ID
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0,        // MIPMAP Level
                GL.GL_RGBA,            // number of color components
                textureImage.getWidth(),    // image size
                textureImage.getHeight(), 0,    // border size in pixels
                GL.GL_RGBA,            // pixel format
                GL.GL_UNSIGNED_BYTE,        // pixel data type
                wrappedRGBA            // buffer holding texture data
        );

        gl.glGenerateMipmap(GL.GL_TEXTURE_2D);

        // enable linear filtering for minification (or else default is MUST use MIPMaps...)
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);

        return textureID;
    }

    /**
     * Read the specified file and return a BufferedImage containing the file's
     * content. The file is assumed to be in a format understood by ImageIO.read
     * (GIF, JPG, PNG, or BMP).
     */
    private BufferedImage getBufferedImage(String fileName) {
        BufferedImage img;
        try {
            img = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            System.err.println("Error reading '" + fileName + '"');
            throw new RuntimeException(e);
        }
        return img;
    }

    /**
     * Extract pixel data from the specified BufferedImage and return it as an
     * an array of bytes in OpenGL-compatible format for use by glDrawPixels().
     * To provide glDrawPixels() with a suitable array, we need to do the
     * following: -Convert to a color model that OpenGL understands. -Use a byte
     * array to keep the color values properly arranged. -Flip the image
     * vertically so that the row order puts the bottom of the image on the
     * first row.
     */
    private byte[] getRGBAPixelData(BufferedImage img) {
        byte[] imgRGBA;
        int height = img.getHeight(null);
        int width = img.getWidth(null);

        // create an (empty) BufferedImage with a suitable Raster and ColorModel
        WritableRaster raster = Raster.createInterleavedRaster(
                DataBuffer.TYPE_BYTE, width, height, 4, null);
        ComponentColorModel colorModel = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 8}, // bits
                true,                // hasAlpha
                false,                // isAlphaPreMultiplied
                ComponentColorModel.TRANSLUCENT,// transparency
                DataBuffer.TYPE_BYTE);        // data transfertype
        BufferedImage newImage = new BufferedImage(colorModel,
                raster, false,            // isRasterPremultiplied
                null);                // properties

        // Since Java expects images to have their origin at the upper left
        // while OpenGL expects the origin at the lower left,
        // we must flip the image upside down prior to extracting the RGBA image data.
        // We create an AffineTransform to perform the flipping,
        // and use the Graphics object for the new image to draw the old image
        // into the new one, applying the AffineTransform as it draws
        // (i.e. "upside down" in the Java sense, which will make it rightside up in the OpenGL sense).

        AffineTransform gt = new AffineTransform();
        gt.translate(0, height);
        gt.scale(1, -1d);

        Graphics2D g = newImage.createGraphics();
        g.transform(gt);

        g.drawImage(img, null, null); // draw original image into new image
        g.dispose();

        // now retrieve the underlying byte array from the raster data buffer
        DataBufferByte dataBuf = (DataBufferByte) raster.getDataBuffer();
        imgRGBA = dataBuf.getData();

        return imgRGBA;
    }
}
