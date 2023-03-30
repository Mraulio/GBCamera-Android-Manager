package com.mraulio.gbcameramanager.gameboycameralib.codecs;

import android.graphics.Bitmap;

import java.io.IOException;

/**
 *
 * @author Raúl Miras Vidal
 */
public interface Codec {
    Bitmap decode(byte[] data);
    Bitmap decodeWithPalette(int paletteIndex,byte[] data);//Added
    byte[] encode(Bitmap image) throws IOException;
    byte[] encodeInternal(Bitmap image) throws IOException;//Added

}