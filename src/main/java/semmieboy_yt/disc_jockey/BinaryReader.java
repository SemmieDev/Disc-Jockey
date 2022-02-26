package semmieboy_yt.disc_jockey;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BinaryReader {
    private final InputStream in;
    private final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);

    public BinaryReader(InputStream in) {
        this.in = in;
    }

    public int readInt() throws IOException {
        return buffer.clear().put(readBytes(Integer.BYTES)).rewind().getInt();
    }

    public long readUInt() throws IOException {
        return readInt() & 0xFFFFFFFFL;
    }

    public int readUShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    public short readShort() throws IOException {
        return buffer.clear().put(readBytes(2)).rewind().getShort();
    }

    public String readString() throws IOException {
        return new String(readBytes(readInt()));
    }

    public float readFloat() throws IOException {
        return buffer.clear().put(readBytes(4)).rewind().getFloat();
    }

    /*private int getStringLength() throws IOException {
        int count = 0;
        int shift = 0;
        boolean more = true;
        while (more) {
            byte b = (byte) in.read();
            count |= (b & 0x7F) << shift;
            shift += 7;
            if ((b & 0x80) == 0) {
                more = false;
            }
        }
        return count;
    }*/

    public byte readByte() throws IOException {
        int b = in.read();
        if (b < 0) throw new EOFException();
        return (byte)(b);
    }

    public byte[] readBytes(int length) throws IOException {
        return in.readNBytes(length);
    }
}
