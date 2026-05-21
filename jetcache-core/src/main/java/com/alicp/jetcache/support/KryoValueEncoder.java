package com.alicp.jetcache.support;

import com.alicp.jetcache.anno.SerialPolicy;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;

import java.util.Arrays;

/**
 * Created on 2016/10/4.
 *
 * Since 2.8.0 the com.esotericsoftware:kryo should be 5+, kryo4 is not supported.
 *
 * @author huangli
 */
public class KryoValueEncoder extends AbstractValueEncoder {

    public static final KryoValueEncoder INSTANCE = new KryoValueEncoder(true);

    private static final int INIT_BUFFER_SIZE = 2048;

    //Default size = 32K
    static ObjectPool<KryoCache> kryoCacheObjectPool = new ObjectPool<>(16, new ObjectPool.ObjectFactory<KryoCache>() {
        @Override
        public KryoCache create() {
            return new KryoCache();
        }

        @Override
        public void reset(KryoCache obj) {
            obj.getKryo().reset();
            obj.getOutput().reset();
        }
    });

    public static class KryoCache {
        final Output output;
        final Kryo kryo;
        public KryoCache(){
            kryo = new Kryo();
            kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
            kryo.setRegistrationRequired(false);
            output = new Output(INIT_BUFFER_SIZE, -1);
        }

        public Output getOutput(){
            return output;
        }
        public Kryo getKryo(){
            return kryo;
        }
    }

    public KryoValueEncoder(boolean useIdentityNumber) {
        super(useIdentityNumber);
    }

    @Override
    public byte[] apply(Object value) {
        KryoCache kryoCache = null;
        try {
            kryoCache = kryoCacheObjectPool.borrowObject();
            Output output = kryoCache.getOutput();
            if (useIdentityNumber) {
                writeInt(output, SerialPolicy.IDENTITY_NUMBER_KRYO5);
            }
            kryoCache.getKryo().writeClassAndObject(output, value);
            return kryoCache.getOutput().toBytes();
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Kryo Encode error. ");
            sb.append("msg=").append(e.getMessage());
            throw new CacheEncodeException(sb.toString(), e);
        } finally {
            if (kryoCache != null) {
                kryoCacheObjectPool.returnObject(kryoCache);
            }
        }
    }

    private void writeInt(Output output, int value) {
        // kryo5 change writeInt to little endian, so we write int manually
        output.writeByte(value >>> 24);
        output.writeByte(value >>> 16);
        output.writeByte(value >>> 8);
        output.writeByte(value);
    }

}
