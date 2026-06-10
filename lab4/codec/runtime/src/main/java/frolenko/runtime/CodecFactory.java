package frolenko.runtime;

import frolenko.annotations.*;
import frolenko.utils.AesUtil;
import frolenko.utils.Crc16;

import java.lang.reflect.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CodecFactory {

    private CodecFactory() {}

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> codecInterface) {
        GeneratedCodec annotation = codecInterface.getAnnotation(GeneratedCodec.class);
        if (annotation == null) {
            throw new IllegalArgumentException(codecInterface.getName() + " is not annotated with @GeneratedCodec");
        }
        Class<?> modelClass = annotation.modelClass();

        return (T) Proxy.newProxyInstance(
                codecInterface.getClassLoader(),
                new Class[]{codecInterface},
                new CodecHandler(modelClass)
        );
    }

    private static class CodecHandler implements InvocationHandler {

        private final Class<?> modelClass;
        private final byte magicByte;
        private final List<Field> headerFields;
        private final Field messageField;
        private final List<Field> messageDefFields;
        private final Field payloadField;

        private CodecHandler(Class<?> modelClass) {
            this.modelClass = modelClass;

            Packet packetAnnotation = modelClass.getAnnotation(Packet.class);
            this.magicByte = packetAnnotation.magicByte();

            this.headerFields = Arrays.stream(modelClass.getDeclaredFields())
                    .filter(f -> f.getAnnotation(frolenko.annotations.Field.class) != null)
                    .sorted(Comparator.comparingInt(f -> f.getAnnotation(frolenko.annotations.Field.class).order()))
                    .toList();

            this.messageField = Arrays.stream(modelClass.getDeclaredFields())
                    .filter(f -> f.getAnnotation(MessageField.class) != null)
                    .findFirst()
                    .orElse(null);

            if (messageField != null) {
                Class<?> messageDef = messageField.getType();

                this.messageDefFields = Arrays.stream(messageDef.getDeclaredFields())
                        .filter(f -> f.getAnnotation(frolenko.annotations.Field.class) != null)
                        .sorted(Comparator.comparingInt(f -> f.getAnnotation(frolenko.annotations.Field.class).order()))
                        .toList();

                this.payloadField = Arrays.stream(messageDef.getDeclaredFields())
                        .filter(f -> f.getAnnotation(Payload.class) != null)
                        .findFirst()
                        .orElse(null);
            } else {
                this.messageDefFields = List.of();
                this.payloadField = null;
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "encode" -> encode(args[0], (byte[]) args[1]);
                case "decode" -> decode((byte[]) args[0], (byte[]) args[1]);
                default -> throw new UnsupportedOperationException("Unknown method: " + method.getName());
            };
        }

        private byte[] encode(Object packet, byte[] key) throws Exception {
            byte[] encryptedPayload = encryptPayload(packet, key);

            int wLen = calcMessageFieldsSize() + (encryptedPayload != null ? encryptedPayload.length : 0);

            byte[] header = buildHeader(packet, wLen);
            short hdrCrc = Crc16.calculateCrc(header);

            byte[] message = buildMessage(packet, encryptedPayload);
            short msgCrc = Crc16.calculateCrc(message);

            int totalSize = header.length + 2 + (message.length > 0 ? message.length + 2 : 0);
            ByteBuffer result = ByteBuffer.allocate(totalSize);
            result.put(header);
            result.putShort(hdrCrc);
            if (message.length > 0) {
                result.put(message);
                result.putShort(msgCrc);
            }
            return result.array();
        }

        private byte[] buildHeader(Object packet, int wLen) throws IllegalAccessException {
            ByteBuffer buffer = ByteBuffer.allocate(1 + calcHeaderFieldsSize() + 4);
            buffer.put(magicByte);
            for (Field f : headerFields) {
                writeField(buffer, f, packet);
            }
            buffer.putInt(wLen);
            return buffer.array();
        }

        private byte[] buildMessage(Object packet, byte[] encryptedPayload) throws IllegalAccessException {
            if (messageField == null) {
                return new byte[0];
            }
            Object messageObj = messageField.get(packet);
            int size = calcMessageFieldsSize() + (encryptedPayload != null ? encryptedPayload.length : 0);
            ByteBuffer buffer = ByteBuffer.allocate(size);
            for (Field f : messageDefFields) {
                writeField(buffer, f, messageObj);
            }
            if (encryptedPayload != null) {
                buffer.put(encryptedPayload);
            }
            return buffer.array();
        }

        private byte[] encryptPayload(Object packet, byte[] key) throws Exception {
            if (messageField == null || payloadField == null) return null;
            Object messageObj = messageField.get(packet);
            Object value = payloadField.get(messageObj);
            if (value == null) return null;
            String plainText = value instanceof byte[]
                    ? new String((byte[]) value, StandardCharsets.UTF_8)
                    : (String) value;
            return AesUtil.encrypt(plainText, key);
        }

        private Object decode(byte[] data, byte[] key) throws Exception {
            ByteBuffer buffer = ByteBuffer.wrap(data);

            if (buffer.get() != magicByte) {
                throw new IllegalArgumentException("Invalid magic byte");
            }

            Object packet = modelClass.getDeclaredConstructor().newInstance();

            for (Field f : headerFields) {
                readField(buffer, f, packet);
            }

            int wLen = buffer.getInt();

            short actualHdrCrc = buffer.getShort();
            short expectedHdrCrc = Crc16.calculateCrc(data, 0, 1 + calcHeaderFieldsSize() + 4);
            if (expectedHdrCrc != actualHdrCrc) {
                throw new IllegalArgumentException("Header CRC mismatch");
            }

            if (messageField == null) return packet;

            Object messageObj = messageField.getType().getDeclaredConstructor().newInstance();
            messageField.set(packet, messageObj);

            for (Field f : messageDefFields) {
                readField(buffer, f, messageObj);
            }

            if (payloadField != null) {
                int payloadSize = wLen - calcMessageFieldsSize();
                byte[] encryptedPayload = new byte[payloadSize];
                buffer.get(encryptedPayload);

                String decrypted = AesUtil.decrypt(encryptedPayload, key);

                if (payloadField.getType() == byte[].class) {
                    payloadField.set(messageObj, decrypted.getBytes(StandardCharsets.UTF_8));
                } else {
                    payloadField.set(messageObj, decrypted);
                }
            }

            int msgStart = 1 + calcHeaderFieldsSize() + 4 + 2;
            short actualMsgCrc = buffer.getShort();
            short expectedMsgCrc = Crc16.calculateCrc(data, msgStart, wLen);
            if (expectedMsgCrc != actualMsgCrc) {
                throw new IllegalArgumentException("Message CRC mismatch");
            }

            return packet;
        }

        private void writeField(ByteBuffer buffer, Field f, Object obj) throws IllegalAccessException {
            Object value = f.get(obj);
            switch (f.getType().getName()) {
                case "byte" -> buffer.put((byte) value);
                case "short" -> buffer.putShort((short) value);
                case "int" -> buffer.putInt((int) value);
                case "long" -> buffer.putLong((long) value);
            }
        }

        private void readField(ByteBuffer buffer, Field f, Object obj) throws IllegalAccessException {
            switch (f.getType().getName()) {
                case "byte" -> f.set(obj, buffer.get());
                case "short" -> f.set(obj, buffer.getShort());
                case "int" -> f.set(obj, buffer.getInt());
                case "long" -> f.set(obj, buffer.getLong());
            }
        }

        private int calcHeaderFieldsSize() {
            return headerFields.stream().mapToInt(f -> primitiveSize(f.getType())).sum();
        }

        private int calcMessageFieldsSize() {
            return messageDefFields.stream().mapToInt(f -> primitiveSize(f.getType())).sum();
        }

        private int primitiveSize(Class<?> type) {
            if (type == byte.class) return 1;
            if (type == short.class) return 2;
            if (type == int.class) return 4;
            if (type == long.class) return 8;
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
    }
}