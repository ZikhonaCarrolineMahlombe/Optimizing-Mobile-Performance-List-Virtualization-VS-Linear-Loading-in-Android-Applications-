package com.group7.renderbench;

import org.junit.Test;
import static org.junit.Assert.*;

import com.group7.renderbench.generators.DataGenerator;
import com.group7.renderbench.entities.Item;
import java.util.List;

public class DataGeneratorTest {

    @Test
    public void testGenerationSize() {
        DataGenerator generator = new DataGenerator();
        List<Item> result = generator.generateDataset(5);

        assertEquals(5, result.size());
    }

    @Test
    public void testItemValues() {
        DataGenerator generator = new DataGenerator();
        List<Item> result = generator.generateDataset(3);

        assertEquals(0, result.get(0).getId());
        assertEquals("Item 0", result.get(0).getTitle());
        assertEquals("Description for item 0", result.get(0).getDescription());
    }
}