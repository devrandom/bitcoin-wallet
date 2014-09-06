package co.cryptocorp.oracle.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.bitcoin.crypto.DeterministicKey;

import java.io.IOException;

/**
* @author devrandom
*/
public class DeterministicKeySerializer extends ToStringSerializer {
    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeString(((DeterministicKey)value).serializePubB58());
    }
}
