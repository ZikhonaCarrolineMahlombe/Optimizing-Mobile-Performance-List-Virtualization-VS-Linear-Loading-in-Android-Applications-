package com.group7.renderbench.generators;

import com.group7.renderbench.entities.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a list of Item objects for the given dataset size.
 * Used by TestController to generate data for each test run.
 */

public class DataGenerator {

    public List<Item> generateDataset(int size) {
        List<Item> items = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            items.add(new Item(
                    i,
                    "Item " + i,
                    "Description for item " + i,
                    null
            ));
        }

        return items;
    }
}