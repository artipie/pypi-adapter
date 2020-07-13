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
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.rq.RequestLine;
import io.reactivex.Flowable;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link SliceIndex}.
 * @since 0.4
 * @todo #41:30min Extend this test: check response status and header, verify cases with different
 *  structure in the storage, do not forget about case with empty storage.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class SliceIndexTest {

    @Test
    void returnsIndexList() {
        final Storage storage = new InMemoryStorage();
        storage.save(new Key.From("abc", "abc-0.1.tar.gz"), new Content.From(new byte[]{})).join();
        storage.save(new Key.From("abc", "abc-0.1.whl"), new Content.From(new byte[]{})).join();
        MatcherAssert.assertThat(
            new SliceIndex(storage).response(
                new RequestLine("GET", "/").toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasBody(
                // @checkstyle LineLengthCheck (1 line)
                "<!DOCTYPE html>\n<html>\n  </body>\n<a href=\"/abc\">abc</a><br/>\n</body>\n</html>".getBytes()
            )
        );
    }

}
