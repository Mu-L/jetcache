package com.alicp.jetcache.support;


import com.alibaba.fastjson2.JSON;
import com.alicp.jetcache.anno.SerialPolicy;

/**
 * Created on 2022/07/26.
 *
 * @author huangli
 */
public class Fastjson2ValueEncoder extends AbstractJsonEncoder {

    public static final Fastjson2ValueEncoder INSTANCE = new Fastjson2ValueEncoder(true);

    public Fastjson2ValueEncoder(boolean useIdentityNumber) {
        super(useIdentityNumber, SerialPolicy.IDENTITY_NUMBER_FASTJSON2);
    }

    @Override
    protected byte[] encodeSingleValue(Object value) {
        return JSON.toJSONBytes(value);
    }

}
