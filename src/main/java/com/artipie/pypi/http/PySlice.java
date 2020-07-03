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

import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicIdentities;
import com.artipie.http.auth.Identities;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.auth.SliceAuth;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.ContentType;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.http.slice.SliceWithHeaders;

/**
 * PyPi HTTP entry point.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class PySlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param storage The storage and default parameters for free access.
     */
    public PySlice(final Storage storage) {
        this(storage, Permissions.FREE, Identities.ANONYMOUS);
    }

    /**
     * Ctor used by Artipie server which knows `Authentication` implementation.
     * @param storage The storage and default parameters for free access.
     * @param perms Access permissions.
     * @param auth Auth details.
     */
    public PySlice(final Storage storage, final Permissions perms, final Authentication auth) {
        this(storage, perms, new BasicIdentities(auth));
    }

    /**
     * Primary ctor.
     * @param storage The storage.
     * @param perms Access permissions.
     * @param auth Concrete identities.
     */
    private PySlice(final Storage storage, final Permissions perms, final Identities auth) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByMethod(RqMethod.GET),
                        new RtRule.ByPath("^[a-zA-Z0-9]*.*\\.whl")
                    ),
                    new SliceAuth(
                        new SliceWithHeaders(
                            new LoggingSlice(new SliceDownload(storage)),
                            new Headers.From(new ContentType("application/octet-stream"))
                        ),
                        new Permission.ByName("download", perms),
                        auth
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByMethod(RqMethod.GET),
                        new RtRule.ByPath("^[a-zA-Z0-9]*.*\\.gz")
                    ),
                    new SliceAuth(
                        new SliceWithHeaders(
                            new LoggingSlice(new SliceDownload(storage)),
                            new Headers.From(new ContentType("application/octet-stream"))
                        ),
                        new Permission.ByName("download", perms),
                        auth
                    )
                ),
                new RtRulePath(
                    new RtRule.ByMethod(RqMethod.POST),
                    new SliceAuth(
                        new WheelSlice(storage),
                        new Permission.ByName("upload", perms),
                        auth
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByMethod(RqMethod.GET),
                        new RtRule.ByPath("//")
                    ),
                    new SliceAuth(
                        new SliceWithHeaders(
                            new LoggingSlice(new SliceIndex()),
                            new Headers.From(new ContentType("text/html"))
                        ),
                        new Permission.ByName("download", perms),
                        auth
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new SliceSimple(new RsWithStatus(RsStatus.NOT_FOUND))
                )
            )
        );
    }
}
