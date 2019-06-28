package me.mizhoux.antenna.imgproc.yuv;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * ByteBufferOutputStream
 *
 * @author Mi Zhou <mizhoux@qq.com>
 */
public final class ByteBufferOutputStream extends ByteArrayOutputStream {

    public ByteBufferOutputStream(int initialSize) {
        super(initialSize);
    }

    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(buf, 0, count);
    }

    // 为了加快处理速度，没有复制数组，所以，千万不要修改这个返回的数组
    public byte[] getByteArray() {
        return buf;
    }

    /**
     * 返回当前缓冲区包含的字节数
     *
     * @return 当前缓冲区包含的字节数
     */
    public int size() {
        return count;
    }

}
