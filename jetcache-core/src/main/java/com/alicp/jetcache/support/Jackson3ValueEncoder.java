package com.alicp.jetcache.support;


import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author huangli
 */
public class Jackson3ValueEncoder extends AbstractJsonEncoder {

    public static final Jackson3ValueEncoder INSTANCE = new Jackson3ValueEncoder(true);

    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    public Jackson3ValueEncoder(boolean useIdentityNumber) {
        super(useIdentityNumber, DecoderMap.IDENTITY_NUMBER_JACKSON3);
    }

    @Override
    protected byte[] encodeSingleValue(Object value) {
        return objectMapper.writeValueAsBytes(value);
    }

}
