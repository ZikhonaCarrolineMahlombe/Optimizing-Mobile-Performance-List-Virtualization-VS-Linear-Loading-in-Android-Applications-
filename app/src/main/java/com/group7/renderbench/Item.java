package com.group7.renderbench;

public class Item {
    private int id;
    private String title;
    private String description;
    private String imageUrl;

    public Item (int id, String title, String description, String imageUrl){
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public int getId(){
       return id;
    }

    public String getTitle(){
        return title;
    }

    public String getDescription(){
        return description;
    }

    public String getImageUrl(){
        return imageUrl;
    }
}