package com.alicp.jetcache.support;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * @author huangli
 */
public class Jackson3ValueDecoder extends AbstractJsonDecoder {

    public static final Jackson3ValueDecoder INSTANCE = new Jackson3ValueDecoder(true);

    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    public Jackson3ValueDecoder(boolean useIdentityNumber) {
        super(useIdentityNumber);
    }

    @Override
    protected Object parseObject(byte[] buffer, int index, int len, Class clazz) {
        String s = new String(buffer, index, len, StandardCharsets.UTF_8);
        return objectMapper.readValue(s, clazz);
    }
}
