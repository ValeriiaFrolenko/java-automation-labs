import frolenko.runtime.CodecFactory;
import model.Message;
import model.MyPacket;
import model.MyPacketCodec;

public class Main {
    public static void main(String[] args) {
        MyPacketCodec codec = CodecFactory.create(MyPacketCodec.class);

        MyPacket packet = new MyPacket();
        packet.bSrc = 0x01;
        packet.bPktId = 123L;

        Message message = new Message();
        message.cType = 1;
        message.bUserId = 42;
        message.payload = "hello";
        packet.message = message;

        byte[] key = "1234567890123456".getBytes();
        byte[] encoded = codec.encode(packet, key);
        MyPacket decoded = codec.decode(encoded, key);

        System.out.println("Original: " + packet.message.payload);
        System.out.println("Decoded: " + decoded.message.payload);
    }
}
