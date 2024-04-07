package com.mraulio.gbcameramanager.gameboycameralib.codecs;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;

import java.io.ByteArrayOutputStream;

/**
 * Modified from https://github.com/KodeMunkie/gameboycameralib
 */
public class ImageCodec implements Codec {

    private final int imageWidth;
    private final int imageHeight;
    private int paletteIndex;

    public ImageCodec(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    @Override
    public Bitmap decode(byte[] data) {
        Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        IndexedPalette ip = new IndexedPalette(Utils.gbcPalettesList.get(paletteIndex).getPaletteColorsInt());
        Codec tileCodec = new TileCodec(ip);
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

    public Bitmap decodeWithPalette(int[] imagePalette, byte[] data, boolean invertImagePalette) {

        Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

        TileCodec tileCodec = new TileCodec();
        Canvas canvas = new Canvas(image);
        int xPos = 0;
        int yPos = 0;
        for (int i = 0; i < data.length; i += TileCodec.TILE_BYTES_LENGTH) {
            byte[] tileData = new byte[TileCodec.TILE_BYTES_LENGTH];
            System.arraycopy(data, i, tileData, 0, TileCodec.TILE_BYTES_LENGTH);
            Bitmap tile = tileCodec.decodeWithPalette(imagePalette, tileData, invertImagePalette, false);
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

    public byte[] encodeInternal(Bitmap buf, String paletteId) {
        //I had an error here, need to select the palette index from the actual image.
        //Also need to change the frame palette alongside this so the colors are the same
        IndexedPalette ip = new IndexedPalette(Utils.hashPalettes.get(paletteId).getPaletteColorsInt());
        Codec tileCodec = new TileCodec(ip);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int y = 0; y + TileCodec.TILE_HEIGHT <= buf.getHeight(); y += TileCodec.TILE_HEIGHT) {
            for (int x = 0; x + TileCodec.TILE_WIDTH <= buf.getWidth(); x += TileCodec.TILE_WIDTH) {
                try {
                    baos.write(tileCodec.encode(Bitmap.createBitmap(buf, x, y, TileCodec.TILE_WIDTH, TileCodec.TILE_HEIGHT)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return baos.toByteArray();
    }
}
