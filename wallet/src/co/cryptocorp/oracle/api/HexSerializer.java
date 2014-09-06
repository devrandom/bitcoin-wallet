package co.cryptocorp.oracle.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

/**
* @author devrandom
*/
public class HexSerializer extends ToStringSerializer {
    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeString(new String(Hex.encode((byte[]) value)));
    }
}
