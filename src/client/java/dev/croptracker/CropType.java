package dev.croptracker;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

enum CropType {
    NETHER_WART("Nether wart", Items.NETHER_WART),
    CARROT("Carrot", Items.CARROT),
    POTATO("Potato", Items.POTATO),
    WHEAT("Wheat", Items.WHEAT),
    BEETROOT("Beetroot", Items.BEETROOT),
    COCOA_BEANS("Cocoa beans", Items.COCOA_BEANS),
    SWEET_BERRIES("Sweet berries", Items.SWEET_BERRIES);

    private final String label;
    private final Item item;

    CropType(String label, Item item) {
        this.label = label;
        this.item = item;
    }

    String label() { return label; }
    Item item() { return item; }

    static Optional<CropType> from(Item item) {
        return Arrays.stream(values()).filter(type -> type.item == item).findFirst();
    }
}
