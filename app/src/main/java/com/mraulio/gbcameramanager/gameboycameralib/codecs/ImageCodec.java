package com.mraulio.gbcameramanager.gameboycameralib.codecs;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;


public class ImageCodec implements Codec {

    private final int imageWidth;
    private final int imageHeight;
    private final IndexedPalette palette;

    public ImageCodec(IndexedPalette palette, int imageWidth, int imageHeight) {
        this.palette = palette;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    @Override
    public Bitmap decode(byte[] data) {
        Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

        Codec tileCodec = new TileCodec(palette);
        Canvas canvas = new Canvas(image);
        int xPos = 0;
        int yPos = 0;
        for (int i = 0; i < data.length; i += TileCodec.TILE_BYTES_LENGTH) {
            byte[] tileData = new byte[TileCodec.TILE_BYTES_LENGTH];
            System.arraycopy(data, i, tileData, 0, TileCodec.TILE_BYTES_LENGTH);
            Bitmap tile = tileCodec.decode(tileData);
            canvas.drawBitmap(tile, xPos, yPos, null);
            xPos += TileCodec.TILE_WIDTH;
            if (xPos >= imageWidth) {
                xPos = 0;
                yPos += TileCodec.TILE_HEIGHT;
            }
            // Failsafe
            if (yPos >= imageHeight) {
                break;
            }
        }
        return image;
    }

    @Override
    public byte[] encode(Bitmap buf) {
        // This method is not used for now.
        return null;
    }
}
