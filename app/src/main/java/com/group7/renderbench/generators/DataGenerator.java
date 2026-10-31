package com.group7.renderbench.generators;

import com.group7.renderbench.entities.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the test data used in each performance run.
 * The DataGenerator produces a list of Item objects of a given size,
 * which is then rendered by the ScrollView or RecyclerView engine.
 * It is called by the TestController during test setup.
 */
public class DataGenerator {

    /**
     * Creates a list of Item objects for the given dataset size.
     * Each item is created with a unique id, a generated title,
     * a generated description, and a null image URL.
     *
     * @param size the number of items to generate
     * @return a list containing exactly {@code size} Item objects
     */
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