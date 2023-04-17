package com.mraulio.gbcameramanager.gameboycameralib.codecs;

import android.graphics.Bitmap;

import com.mraulio.gbcameramanager.model.GbcImage;

import java.io.IOException;

/**
 *
 * @author Raúl Miras Vidal
 */
public interface Codec {
    Bitmap decode(byte[] data);
    Bitmap decodeWithPalette(int[] palette,byte[] data);//Added
    byte[] encode(Bitmap image) throws IOException;
    byte[] encodeInternal(Bitmap image) throws IOException;//Added

}