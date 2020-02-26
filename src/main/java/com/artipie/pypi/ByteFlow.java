/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.pypi;

import com.artipie.asto.Remaining;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.reactivestreams.Publisher;

/**
 * ByteFlow.
 *
 * @since 0.1
 */
public final class ByteFlow implements Flow<ByteBuffer> {
    /**
     * Data for flow.
     */
    private final ByteBuffer data;

    /**
     * Ctor.
     *
     * @param data Data for flow.
     */
    public ByteFlow(final String data) {
        this(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Ctor.
     *
     * @param data Data for flow.
     */
    public ByteFlow(final ByteBuffer data) {
        this.data = data;
    }

    @Override
    public Publisher<ByteBuffer> value() {
        return Flowable.fromArray(this.data);
    }

    @Override
    public String toString() {
        return new String(new Remaining(this.data).bytes());
    }
}
