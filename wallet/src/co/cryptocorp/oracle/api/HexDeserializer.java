package co.cryptocorp.oracle.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;

import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

/**
* @author devrandom
*/
public class HexDeserializer extends FromStringDeserializer<byte[]> {

    public HexDeserializer() {
        super(byte[].class);
    }

    @Override
    protected byte[] _deserialize(String value, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return Hex.decode(value);
    }
}
