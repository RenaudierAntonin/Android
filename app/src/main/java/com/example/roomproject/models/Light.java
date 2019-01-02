package com.example.roomproject.models;

public class Light {

    private Long id;

    private Integer level;

    private Status status;

    private Long roomId;

    private String color;

    private String brightness;

    public Light() {
    }

    public Light(Integer level, Status status, Long roomId, String color, String brightness) {
        this.level = level;
        this.status = status;
        this.roomId = roomId;
        this.color = color;
        this.brightness = brightness;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBrightness() {
        return brightness;
    }

    public void setBrightness(String brightness) {
        this.brightness = brightness;
    }
}
