package com.group7.listperformance;

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

    public void setId(int id){
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageUrl(){
        this.imageUrl = imageUrl;
    }
}