package me.tuskdev.generator.listener;

import me.tuskdev.generator.cache.GeneratorCache;
import me.tuskdev.generator.config.GeneratorManager;
import me.tuskdev.generator.config.data.SimpleGenerator;
import me.tuskdev.generator.controller.GeneratorController;
import me.tuskdev.generator.model.Generator;
import me.tuskdev.generator.util.Coordinates;
import me.tuskdev.generator.util.GeneratorUtil;
import me.tuskdev.generator.util.ItemNBT;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BlockPlaceListener implements Listener {

    private final GeneratorCache generatorCache;
    private final GeneratorController generatorController;
    private final GeneratorManager generatorManager;
    private final String cantPlaceGenerator;

    public BlockPlaceListener(GeneratorCache generatorCache, GeneratorController generatorController, GeneratorManager generatorManager, String cantPlaceGenerator) {
        this.generatorCache = generatorCache;
        this.generatorController = generatorController;
        this.generatorManager = generatorManager;
        this.cantPlaceGenerator = ChatColor.translateAlternateColorCodes('&', cantPlaceGenerator);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();
        if (itemStack == null || !ItemNBT.has(itemStack, "type")) return;
        event.setCancelled(true);

        String type = ItemNBT.get(itemStack, "type");
        SimpleGenerator simpleGenerator = generatorManager.getGenerator(type);

        UUID owner = event.getPlayer().getUniqueId();
        Location location = event.getBlock().getLocation().add(0, 1, 0);
        int level = ItemNBT.has(itemStack, "level") ? Integer.parseInt(ItemNBT.get(itemStack, "level")) : 1;

        Generator target = generatorCache.selectIf(coordinates -> location.distance(coordinates.build()) < 5);
        if (target != null) {
            event.getPlayer().sendMessage(cantPlaceGenerator);
            return;
        }

        if (itemStack.getAmount() > 1) itemStack.setAmount(itemStack.getAmount() - 1);
        else event.getPlayer().setItemInHand(new ItemStack(Material.AIR));
        Generator generator = new Generator(owner, Coordinates.of(location), type);
        generator.setLevel(level);
        generator.setTime(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(simpleGenerator.getItemsDelay()));

        generatorCache.insert(generator);
        generatorController.insert(generator);

        GeneratorUtil.buildStruct(location);
        GeneratorUtil.hologram(generator, simpleGenerator);
    }

}
