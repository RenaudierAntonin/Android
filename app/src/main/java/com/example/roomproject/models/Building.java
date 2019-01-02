package com.example.roomproject.models;

public class Building {


    public Building() {

    }

    public Building(String name) {
        this.name = name;
    }

    private Long id;

    private String name;


    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
