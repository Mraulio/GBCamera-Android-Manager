package com.mraulio.gbcameramanager.gameboycameralib.saveExtractor;


import android.graphics.Bitmap;

import java.io.File;
import java.io.IOException;
import java.util.List;
/**
 * Modified from https://github.com/KodeMunkie/gameboycameralib

 * Interface for extracting raw 2bpp image(s) data from different source mediums, e.g
 * Gameboy save file or Gameboy printer dump, and returns the decoded images in the method
 * specified format.
 */
public interface Extractor {

    /**
     * Extract images from a local file system file
     *
     * @param file the file to read from
     * @return the BufferedImage representation(s)
     * @throws IOException if the file fails
     */
    List<Bitmap> extract(File file) throws IOException;

    /**
     * Extract images from a raw byte data stream
     *
     * @param rawData the raw byte data to read from
     * @return the BufferedImage representation(s)
     */
    List<Bitmap> extract(byte[] rawData);




    //Added by Mraulio
    List<byte[]> extractBytes(byte[] rawData);
    List<byte[]> extractBytes(File file) throws IOException;

    /**
     * Extract images from a local file system file and return as PNG byte data
     *
     * @param file the file to read from
     * @return the PNG representation(s)
     * @throws IOException if the file fails
     */
    List<byte[]> extractAsPng(File file) throws IOException;

    /**
     * Extract images from a raw byte data stream and return as PNG byte data
     *
     * @param rawData the raw byte data to read from
     * @return the PNG representation(s)
     */
    List<byte[]> extractAsPng(byte[] rawData);
}
