package com.example.clickngo.models;

public class Link {
    private String name;
    private String link;

    public Link() {
        // Default constructor required for calls to DataSnapshot.getValue(Link.class)
    }

    public Link(String name, String link) {
        this.name = name;
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }
}

