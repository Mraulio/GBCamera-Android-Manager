package com.mraulio.gbcameramanager.gameboycameralib.codecs;

import android.graphics.Bitmap;

import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;


/**
 * Codec para decodificar y codificar una imagen de mosaico de 8x8 píxeles
 * a un conjunto de datos de bytes y viceversa.
 *
 * Este codec utiliza una paleta de colores indexados para decodificar
 * y codificar los datos de mosaico.
 *
 */
public class TileCodec implements Codec {

    private static final int ROW_BYTES = 2;
    public static final int TILE_WIDTH = 8;
    public static final int TILE_HEIGHT = 8;
    public static final int TILE_BYTES_LENGTH = 16;

    private final IndexedPalette palette;

    public TileCodec(IndexedPalette palette) {
        this.palette = palette;
    }

    @Override
    public Bitmap decode(byte[] tileData) {
        Bitmap buf = Bitmap.createBitmap(TILE_WIDTH, TILE_HEIGHT, Bitmap.Config.ARGB_8888);
        for (int y=0; y<TILE_HEIGHT; y++) {
            byte lowByte = reverseBitEndianess(tileData[y*ROW_BYTES]);
            byte highByte = reverseBitEndianess(tileData[y*ROW_BYTES+1]);
            for (int x=0; x<TILE_WIDTH; x++) {
                int paletteIndex = getPaletteIndex(getBit(lowByte, x), getBit(highByte, x));
                buf.setPixel(x, y, palette.getRGB(paletteIndex));
            }
        }
        return buf;
    }

    @Override
    public byte[] encode(Bitmap buf) {
        byte[] result = new byte[TILE_BYTES_LENGTH];
        for (int y=0; y<TILE_HEIGHT; y++) {
            byte lowByte = (byte)0;
            byte highByte = (byte)0;
            for (int x=0; x<TILE_WIDTH; x++) {
                int rgb = buf.getPixel(x,y);
                int index = palette.getIndex(rgb);
                byte lowBit = getBit((byte)index,0);
                byte highBit = getBit((byte)index,1);
                if (lowBit == 1) {
                    lowByte = setBit(lowByte, x);
                }
                if (highBit == 1) {
                    highByte = setBit(highByte, x);
                }
            }
            lowByte = reverseBitEndianess(lowByte);
            highByte = reverseBitEndianess(highByte);
            result[y*ROW_BYTES] = lowByte;
            result[y*ROW_BYTES+1] = highByte;
        }
        return result;
    }

    public byte setBit(byte b, int position) {
        return (byte) (b | (1 << position));
    }

    public byte getBit(byte b, int position) {
        return (byte)((b >> position) & 1);
    }

    public int getPaletteIndex(byte lowBit, byte highBit) {
        int index = 0;
        index |= highBit << 1;
        index |= lowBit;
        return index;
    }
    /**
     * Los datos transmitidos en GB (dump de memoria o impresora) tienen bits con endianness invertido
     * @param byteToReverse
     * @return byte con bits invertidos
     */
    public static byte reverseBitEndianess(byte byteToReverse) {
        byte b = 0;
        for (int i = 0; i < 8; ++i) {
            b <<= 1;
            b |= byteToReverse & 1;
            byteToReverse >>= 1;
        }
        return b;
    }
}
