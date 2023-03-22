package com.mraulio.gbcameramanager.gameboycameralib.codecs;

import android.graphics.Bitmap;

import java.io.IOException;

/**
 *
 * @author Raúl Miras Vidal
 */
public interface Codec {
    Bitmap decode(byte[] data);
    byte[] encode(Bitmap image) throws IOException;
}