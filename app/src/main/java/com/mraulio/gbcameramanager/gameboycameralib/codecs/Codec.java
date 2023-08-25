package com.mraulio.gbcameramanager.gameboycameralib.codecs;

import android.graphics.Bitmap;

import com.mraulio.gbcameramanager.model.GbcImage;

import java.io.IOException;

/**
 * Modified from https://github.com/KodeMunkie/gameboycameralib
 */
public interface Codec {
    Bitmap decode(byte[] data);
    byte[] encode(Bitmap image) throws IOException;
    byte[] encodeInternal(Bitmap image,String paletteId) throws IOException;//Added

}