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

import com.artipie.http.Headers;
import com.artipie.http.hm.IsHeader;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RedirectSlice}.
 * @since 0.6
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RedirectSliceTest {

    @Test
    void redirectsToNormalizedName() {
        MatcherAssert.assertThat(
            new RedirectSlice().response(
                new RequestLine(RqMethod.GET, "/one/two/three_four").toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new ResponseMatcher(
                RsStatus.MOVED_PERMANENTLY,
                new IsHeader("Location", "/one/two/three-four")
            )
        );
    }

    @Test
    void redirectsToNormalizedNameWithSlashAtTheEnd() {
        MatcherAssert.assertThat(
            new RedirectSlice().response(
                new RequestLine(RqMethod.GET, "/one/two/three_four/").toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new ResponseMatcher(
                RsStatus.MOVED_PERMANENTLY,
                new IsHeader("Location", "/one/two/three-four")
            )
        );
    }

    @Test
    void redirectsToNormalizedNameWhenFillPathIsPresent() {
        MatcherAssert.assertThat(
            new RedirectSlice().response(
                new RequestLine(RqMethod.GET, "/three/F.O.U.R").toString(),
                new Headers.From("X-FullPath", "/one/two/three/F.O.U.R"),
                Flowable.empty()
            ),
            new ResponseMatcher(
                RsStatus.MOVED_PERMANENTLY,
                new IsHeader("Location", "/one/two/three/f-o-u-r")
            )
        );
    }

    @Test
    void normalizesOnlyLastPart() {
        MatcherAssert.assertThat(
            new RedirectSlice().response(
                new RequestLine(RqMethod.GET, "/three/One_Two").toString(),
                new Headers.From("X-FullPath", "/One_Two/three/One_Two"),
                Flowable.empty()
            ),
            new ResponseMatcher(
                RsStatus.MOVED_PERMANENTLY,
                new IsHeader("Location", "/One_Two/three/one-two")
            )
        );
    }

}
