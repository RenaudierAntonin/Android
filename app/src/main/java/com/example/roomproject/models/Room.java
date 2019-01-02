package com.example.roomproject.models;

import java.util.List;


public class Room {

    private Long id;

    private String name;

    private Long floor;

    private Long buildingId;

    private Status status;

    public Room(String name, Long floor, Long buildingId, Status status) {
        this.name = name;
        this.floor = floor;
        this.buildingId = buildingId;
        this.status = status;
    }

    public Room (){

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getFloor() {
        return floor;
    }

    public void setFloor(Long floor) {
        this.floor = floor;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
