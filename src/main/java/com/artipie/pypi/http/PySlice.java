/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/python-adapter/LICENSE.txt
 */

package com.artipie.pypi.http;

import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.http.slice.SliceWithHeaders;
import java.util.regex.Pattern;

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
        this(storage, Permissions.FREE, Authentication.ANONYMOUS);
    }

    /**
     * Primary ctor.
     * @param storage The storage.
     * @param perms Access permissions.
     * @param auth Concrete identities.
     */
    public PySlice(final Storage storage, final Permissions perms, final Authentication auth) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath(".*\\.(whl|tar\\.gz|zip|tar\\.bz2|tar\\.Z|tar|egg)")
                    ),
                    new BasicAuthSlice(
                        new SliceWithHeaders(
                            new SliceDownload(storage),
                            new Headers.From(new ContentType("application/octet-stream"))
                        ),
                        auth,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.POST),
                        new RtRule.ByHeader(
                            "content-type", Pattern.compile("multipart.*", Pattern.CASE_INSENSITIVE)
                        )
                    ),
                    new BasicAuthSlice(
                        new WheelSlice(storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.WRITE)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.POST),
                        new RtRule.ByHeader(
                            "content-type", Pattern.compile("text.*", Pattern.CASE_INSENSITIVE)
                        )
                    ),
                    new BasicAuthSlice(
                        new SearchSlice(storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.WRITE)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath("(^\\/)|(.*(\\/[a-z0-9\\-]+?\\/?$))")
                    ),
                    new BasicAuthSlice(
                        new SliceIndex(storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET)
                    ),
                    new BasicAuthSlice(
                        new RedirectSlice(),
                        auth,
                        new Permission.ByName(perms, Action.Standard.READ)
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
