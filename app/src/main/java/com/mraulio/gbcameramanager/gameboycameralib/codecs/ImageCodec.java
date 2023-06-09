package com.mraulio.gbcameramanager.gameboycameralib.codecs;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.mraulio.gbcameramanager.methods.Methods;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;

import java.io.ByteArrayOutputStream;


public class ImageCodec implements Codec {

    private final int imageWidth;
    private final int imageHeight;
    private IndexedPalette palette = new IndexedPalette(Methods.gbcPalettesList.get(1).getPaletteColorsInt());
    private int paletteIndex;

    public ImageCodec(IndexedPalette palette, int imageWidth, int imageHeight) {
        this.palette = palette;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    @Override
    public Bitmap decode(byte[] data) {
        Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        IndexedPalette ip = new IndexedPalette(Methods.gbcPalettesList.get(paletteIndex).getPaletteColorsInt());
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
    public Bitmap decodeWithPalette(int[] palette, byte[] data) {
        System.out.println("width"+imageWidth+",height:"+imageHeight);
        Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        IndexedPalette ip = new IndexedPalette(Methods.gbcPalettesList.get(paletteIndex).getPaletteColorsInt());

        Codec tileCodec = new TileCodec(ip);
        Canvas canvas = new Canvas(image);
        int xPos = 0;
        int yPos = 0;
        for (int i = 0; i < data.length; i += TileCodec.TILE_BYTES_LENGTH) {
            byte[] tileData = new byte[TileCodec.TILE_BYTES_LENGTH];
            System.arraycopy(data, i, tileData, 0, TileCodec.TILE_BYTES_LENGTH);
            Bitmap tile = tileCodec.decodeWithPalette(palette,tileData);
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

    public byte[] encodeInternal(Bitmap buf) {
        //I had an error here, need to select the palette index from the actual image.
        //Also need to change the frame palette alongside this so the colors are the same
        IndexedPalette ip = new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColorsInt());
        Codec tileCodec = new TileCodec(ip);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int y=0; y+TileCodec.TILE_HEIGHT<=buf.getHeight(); y+=TileCodec.TILE_HEIGHT) {
            for (int x=0; x+TileCodec.TILE_WIDTH<=buf.getWidth(); x+=TileCodec.TILE_WIDTH) {
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
