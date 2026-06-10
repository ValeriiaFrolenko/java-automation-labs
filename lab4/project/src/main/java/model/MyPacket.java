package model;


import frolenko.annotations.Field;
import frolenko.annotations.MessageField;
import frolenko.annotations.Packet;

@Packet(magicByte = 0x7E)
public class MyPacket {
    @Field(order = 1) public byte bSrc;
    @Field(order = 2) public long bPktId;

    @MessageField
    public Message message;

    public MyPacket() {}
}