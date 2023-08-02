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
    private IndexedPalette palette = new IndexedPalette(Utils.gbcPalettesList.get(1).getPaletteColorsInt());
    private int paletteIndex;
    private boolean keepFrame;

    public ImageCodec(int imageWidth, int imageHeight, boolean keepFrame) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.keepFrame = keepFrame;
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

    //    @Override
//    public Bitmap decodeWithPalette(int[] palette, byte[] data) {
//        Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
//        IndexedPalette ip = new IndexedPalette(Utils.gbcPalettesList.get(paletteIndex).getPaletteColorsInt());
//
//        Codec tileCodec = new TileCodec(ip);
//        Canvas canvas = new Canvas(image);
//        int xPos = 0;
//        int yPos = 0;
//        for (int i = 0; i < data.length; i += TileCodec.TILE_BYTES_LENGTH) {
//            byte[] tileData = new byte[TileCodec.TILE_BYTES_LENGTH];
//            System.arraycopy(data, i, tileData, 0, TileCodec.TILE_BYTES_LENGTH);
//            Bitmap tile = tileCodec.decodeWithPalette(palette,tileData);
//            canvas.drawBitmap(tile, xPos, yPos, null);
//            xPos += TileCodec.TILE_WIDTH;
//            if (xPos >= imageWidth) {
//                xPos = 0;
//                yPos += TileCodec.TILE_HEIGHT;
//            }
//            // Failsafe
//            if (yPos >= imageHeight) {
//                break;
//            }
//        }
//        return image;
//    }
    @Override
    public Bitmap decodeWithPalette(int[] palette, byte[] data) {
        if (keepFrame) {
            int regionWidth = 128;
            int regionHeight = 112;
            int startX = (imageWidth - regionWidth) / 2;
            int startY = (imageHeight - regionHeight) / 2;

            Bitmap regionBitmap = Bitmap.createBitmap(regionWidth, regionHeight, Bitmap.Config.ARGB_8888);
            Bitmap externalBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

            IndexedPalette ip = new IndexedPalette(Utils.gbcPalettesList.get(paletteIndex).getPaletteColorsInt());
            IndexedPalette externalPalette = new IndexedPalette(Utils.gbcPalettesList.get(0).getPaletteColorsInt()); // Reemplaza externalPaletteColorsInt con los colores de la paleta externa
            Codec tileCodec = new TileCodec(ip);
            Codec externalTileCodec = new TileCodec(externalPalette);

            Canvas regionCanvas = new Canvas(regionBitmap);
            Canvas externalCanvas = new Canvas(externalBitmap);

            int xPos = 0;
            int yPos = 0;
            for (int i = 0; i < data.length; i += TileCodec.TILE_BYTES_LENGTH) {
                byte[] tileData = new byte[TileCodec.TILE_BYTES_LENGTH];
                System.arraycopy(data, i, tileData, 0, TileCodec.TILE_BYTES_LENGTH);
                Bitmap tile = tileCodec.decodeWithPalette(palette, tileData);

                // Dibujar solo en la región a procesar
                if (xPos >= startX && xPos + TileCodec.TILE_WIDTH <= startX + regionWidth &&
                        yPos >= startY && yPos + TileCodec.TILE_HEIGHT <= startY + regionHeight) {
                    regionCanvas.drawBitmap(tile, xPos - startX, yPos - startY, null);
                } else {
                    // Dibujar en la parte externa con la paleta externa
                    externalCanvas.drawBitmap(externalTileCodec.decodeWithPalette(Utils.gbcPalettesList.get(0).getPaletteColorsInt(), tileData), xPos, yPos, null);
                }

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

            // Combinar los dos bitmaps
            Bitmap combinedBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
            Canvas combinedCanvas = new Canvas(combinedBitmap);
            combinedCanvas.drawBitmap(regionBitmap, startX, startY, null);
            combinedCanvas.drawBitmap(externalBitmap, 0, 0, null);

            return combinedBitmap;
        } else {
            Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
            IndexedPalette ip = new IndexedPalette(Utils.gbcPalettesList.get(paletteIndex).getPaletteColorsInt());

            Codec tileCodec = new TileCodec(ip);
            Canvas canvas = new Canvas(image);
            int xPos = 0;
            int yPos = 0;
            for (int i = 0; i < data.length; i += TileCodec.TILE_BYTES_LENGTH) {
                byte[] tileData = new byte[TileCodec.TILE_BYTES_LENGTH];
                System.arraycopy(data, i, tileData, 0, TileCodec.TILE_BYTES_LENGTH);
                Bitmap tile = tileCodec.decodeWithPalette(palette, tileData);
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
                    // Can likely be ignored for this in memory stream type
                    e.printStackTrace();
                }
            }
        }
        return baos.toByteArray();
    }
}
