/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 NBCO Yandex.Money LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.yandex.money.api.typeadapters.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.yandex.money.api.model.DigitalGoods;
import com.yandex.money.api.typeadapters.BaseTypeAdapter;

import java.lang.reflect.Type;

/**
 * Type adapter for {@link DigitalGoods}.
 *
 * @author Anton Ermak (ermak@yamoney.ru)
 */
public final class DigitalGoodsTypeAdapter extends BaseTypeAdapter<DigitalGoods> {

    private static final DigitalGoodsTypeAdapter INSTANCE = new DigitalGoodsTypeAdapter();

    private static final String MEMBER_ARTICLE = "article";
    private static final String MEMBER_BONUS = "bonus";

    private DigitalGoodsTypeAdapter() {
    }

    /**
     * @return instance of this class
     */
    public static DigitalGoodsTypeAdapter getInstance() {
        return INSTANCE;
    }

    @Override
    public DigitalGoods deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject object = json.getAsJsonObject();
        return new DigitalGoods(GoodTypeAdapter.getInstance().fromJson(object.getAsJsonArray(MEMBER_ARTICLE)),
                GoodTypeAdapter.getInstance().fromJson(object.getAsJsonArray(MEMBER_BONUS)));
    }

    @Override
    public JsonElement serialize(DigitalGoods src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.add(MEMBER_ARTICLE, GoodTypeAdapter.getInstance().toJsonArray(src.article));
        object.add(MEMBER_BONUS, GoodTypeAdapter.getInstance().toJsonArray(src.bonus));
        return object;
    }

    @Override
    protected Class<DigitalGoods> getType() {
        return DigitalGoods.class;
    }
}
