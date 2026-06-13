package protocol;

import model.Message;
import model.Packet;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class PacketRoundTripTest {

    private final byte[] key = "1234567890abcdef".getBytes();
    private final PacketEncoder encoder = new PacketEncoder(key);
    private final PacketDecoder decoder = new PacketDecoder(key);

    @TestFactory
    Stream<DynamicTest> decodedPacketFieldsMatchOriginal() throws Exception {
        Packet original = new Packet((byte) 1, 42L, new Message(3, 99, "hello"));
        byte[] encoded = encoder.encode(original);
        Packet decoded = decoder.decode(encoded);

        return Stream.of(
                DynamicTest.dynamicTest("bSrc matches", () ->
                        assertEquals(original.getbSrc(), decoded.getbSrc())),
                DynamicTest.dynamicTest("bPktId matches", () ->
                        assertEquals(original.getbPktId(), decoded.getbPktId())),
                DynamicTest.dynamicTest("cType matches", () ->
                        assertEquals(original.getbMsq().getcType(), decoded.getbMsq().getcType())),
                DynamicTest.dynamicTest("message matches", () ->
                        assertEquals(original.getbMsq().getMessage(), decoded.getbMsq().getMessage()))
        );
    }
}