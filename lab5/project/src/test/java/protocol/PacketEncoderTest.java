package protocol;

import model.Message;
import model.Packet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class PacketEncoderTest {

    private final byte[] key = "1234567890abcdef".getBytes();
    private final PacketEncoder SUT = new PacketEncoder(key);

    @Test
    void shouldThrowExceptionForNullPacketEncode() {
        assertThrows(IllegalArgumentException.class, () -> SUT.encode(null));
    }

    @ParameterizedTest
    @MethodSource("providePacketsForEncoding")
    void shouldEncodePacket(byte src, long pktId, int cType, int userId, String payload) throws Exception {
        Packet packet = new Packet(src, pktId, new Message(cType, userId, payload));
        byte[] encoded = SUT.encode(packet);
        assertNotNull(encoded);
        assertTrue(encoded.length >= PacketStructure.MIN_PACKET_SIZE);
        assertEquals(PacketStructure.MAGIC_BYTE, encoded[0]);
    }

    static Stream<Arguments> providePacketsForEncoding() {
        return Stream.of(
                Arguments.of((byte) 1, 100L, 1, 42, "hello"),
                Arguments.of((byte) 2, 200L, 2, 99, "world"),
                Arguments.of((byte) 0,   0L, 0,  0, "")
        );
    }
}