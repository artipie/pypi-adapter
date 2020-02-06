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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * PackagesMeta.
 *
 * @since 0.1
 */
public final class PackagesMeta implements LiveMeta {

    /**
     * Content of packages meta.
     */
    private final String content;

    /**
     * Ctor.
     *
     * @checkstyle LineLengthCheck (3 lines).
     */
    public PackagesMeta() {
        this("<html><body><table><thead><tr><th>Filename</th></tr></thead><tbody></tbody></table></body></html>");
    }

    /**
     * Ctor.
     *
     * @param content Content of packages meta
     */
    public PackagesMeta(final String content) {
        this.content = content;
    }

    @Override
    public Meta update(final Meta meta) {
        final Document doc = Jsoup.parse(this.content);
        final Element tbody = doc.getElementsByTag("tbody").get(0);
        final List<Element> trs = new ArrayList<>(tbody.getElementsByTag("tr"));
        final Optional<Element> found = trs.stream().filter(
            tr -> meta.html().equals(normalizeHtmlString(tr.outerHtml()))
        ).findFirst();
        if (found.isEmpty()) {
            tbody.append(meta.html());
        }
        return new PackagesMeta(normalizeHtmlString(doc.html()));
    }

    @Override
    public String html() {
        return this.content;
    }

    /**
     * Minimize html string by removing space symbols.
     *
     * @param html HTML string.
     * @return Minimized HTML string.
     */
    private static String normalizeHtmlString(final String html) {
        return html.replaceAll(">\\s+", ">").trim();
    }
}
