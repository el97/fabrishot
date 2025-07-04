/*
 * MIT License
 *
 * Copyright (c) 2021 Ramid Khan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.ramidzkh.fabrishot.capture;

import me.ramidzkh.fabrishot.config.Config;
import me.ramidzkh.fabrishot.event.ScreenshotSaveCallback;
import net.minecraft.client.texture.NativeImage;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImageWrite;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FramebufferWriter {

    public static void write(NativeImage image, Path file) throws IOException {
        try (FileChannel fc = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             WriteCallback callback = new WriteCallback(fc)) {
            switch (Config.CAPTURE_FILE_FORMAT) {
                case PNG -> STBImageWrite.nstbi_write_png_to_func(callback.address(), 0L, image.getWidth(), image.getHeight(), image.getFormat().getChannelCount(), image.imageId(), 0);
                case JPG -> STBImageWrite.nstbi_write_jpg_to_func(callback.address(), 0L, image.getWidth(), image.getHeight(), image.getFormat().getChannelCount(), image.imageId(), 90);
                case TGA -> STBImageWrite.nstbi_write_tga_to_func(callback.address(), 0L, image.getWidth(), image.getHeight(), image.getFormat().getChannelCount(), image.imageId());
                case BMP -> STBImageWrite.nstbi_write_bmp_to_func(callback.address(), 0L, image.getWidth(), image.getHeight(), image.getFormat().getChannelCount(), image.imageId());
            }

            if (callback.exception != null) {
                throw callback.exception;
            }
        }

        ScreenshotSaveCallback.EVENT.invoker().onSaved(file);
    }

    private static class WriteCallback extends STBIWriteCallback implements AutoCloseable, Closeable {
        private final WritableByteChannel channel;
        private IOException exception;

        private WriteCallback(WritableByteChannel channel) {
            this.channel = channel;
        }

        @Override
        public void invoke(long context, long data, int size) {
            if (this.exception != null) {
                return;
            }

            ByteBuffer buf = STBIWriteCallback.getData(data, size);

            try {
                this.channel.write(buf);
            } catch (IOException e) {
                this.exception = e;
            }
        }

        @Override
        public void close() {
            this.free();
        }
    }
}
