package com.mraulio.gbcameramanager.gameboycameralib.codecs;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class ImageCodec implements Codec {

    private final int imageWidth;
    private final int imageHeight;
    private IndexedPalette palette = new IndexedPalette(Methods.gbcPalettesList.get(1).getPaletteColors());
    private int paletteIndex;

    public ImageCodec(IndexedPalette palette, int imageWidth, int imageHeight) {
        this.palette = palette;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    //Added by Mraulio
//    public ImageCodec(int paletteIndex, int imageWidth, int imageHeight) {
//        this.paletteIndex = paletteIndex;
//        this.imageWidth = imageWidth;
//        this.imageHeight = imageHeight;
//    }

    @Override
    public Bitmap decode(byte[] data) {
        Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        IndexedPalette ip = new IndexedPalette(Methods.gbcPalettesList.get(paletteIndex).getPaletteColors());
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

    @Override
    public Bitmap decodeWithPalette(int paletteIndex, byte[] data) {
        Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        IndexedPalette ip = new IndexedPalette(Methods.gbcPalettesList.get(paletteIndex).getPaletteColors());

        Codec tileCodec = new TileCodec(ip);
        Canvas canvas = new Canvas(image);
        int xPos = 0;
        int yPos = 0;
        for (int i = 0; i < data.length; i += TileCodec.TILE_BYTES_LENGTH) {
            byte[] tileData = new byte[TileCodec.TILE_BYTES_LENGTH];
            System.arraycopy(data, i, tileData, 0, TileCodec.TILE_BYTES_LENGTH);
            Bitmap tile = tileCodec.decodeWithPalette(paletteIndex,tileData);
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

    public byte[] encodeInternal(Bitmap buf, GbcImage gbcImage) {
        //I had an error here, need to select the palette index from the actual image.
        //Also need to change the frame palette alongside this so the colors are the same
        IndexedPalette ip = new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors());
        Codec tileCodec = new TileCodec(ip);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int aux = 0;
        for (int y=0; y+TileCodec.TILE_HEIGHT<=buf.getHeight(); y+=TileCodec.TILE_HEIGHT) {
            for (int x=0; x+TileCodec.TILE_WIDTH<=buf.getWidth(); x+=TileCodec.TILE_WIDTH) {
                try {
                    baos.write(tileCodec.encode(Bitmap.createBitmap(buf, x, y, TileCodec.TILE_WIDTH, TileCodec.TILE_HEIGHT)));
                } catch (Exception e) {
                    // Can likely be ignored for this in memory stream type
                    aux++;
                    e.printStackTrace();
                }
            }
        }
        System.out.println("+++++++++++++++++++++++Cuantos exceptions?: "+aux);
        return baos.toByteArray();
    }
}
