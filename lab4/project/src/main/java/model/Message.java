package model;

import frolenko.annotations.*;

@MessageDef
public class Message {
    @Field(order = 1) public int cType;
    @Field(order = 2) public int bUserId;

    @Payload
    public String payload;

    public Message() {}
}