package com.group7.renderbench.entities;

/**
 * Represents a single row of data displayed in the test list.
 * The DataGenerator creates these objects and the ScrollView or
 * RecyclerView engines render them.
 */
public class Item {

    private int id;
    private String title;
    private String description;
    private String imageUrl;

    /**
     * Creates a new Item with the given values.
     * The Item is immutable after creation.
     *
     * @param id the unique identifier for this item
     * @param title the title shown as the first line of the row
     * @param description the description shown as the second line of the row
     * @param imageUrl the optional image URL for the row icon
     */
    public Item(int id, String title, String description, String imageUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    /**
     * Returns the unique identifier of this item.
     *
     * @return the item id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the title of this item.
     *
     * @return the title text
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the description of this item.
     *
     * @return the description text
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the image URL of this item.
     *
     * @return the image URL, or null if none was set
     */
    public String getImageUrl() {
        return imageUrl;
    }
}