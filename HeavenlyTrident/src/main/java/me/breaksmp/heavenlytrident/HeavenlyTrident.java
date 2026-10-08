package me.breaksmp.heavenlytrident;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeavenlyTrident extends JavaPlugin implements Listener {

    private NamespacedKey tridentKey;

    @Override
    public void onEnable() {

        tridentKey = new NamespacedKey(this, "heavenly_trident");

        Bukkit.getPluginManager().registerEvents(this, this);

        registerRecipe();

        getLogger().info("HeavenlyTrident enabled!");
    }

    public ItemStack createHeavenlyTrident() {

        ItemStack item = new ItemStack(Material.TRIDENT);

        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                net.kyori.adventure.text.Component.text("Небесный трезубец")
                        .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                        .decoration(
                                net.kyori.adventure.text.format.TextDecoration.ITALIC,
                                false
                        )
        );

        meta.getPersistentDataContainer().set(
                tridentKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.setItemModel(
                new NamespacedKey("breaksmp", "trident_staff")
        );

        item.setItemMeta(meta);

        return item;
    }

    private boolean isHeavenlyTrident(ItemStack item) {

        if (item == null || item.getType() != Material.TRIDENT) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        return meta.getPersistentDataContainer().has(
                tridentKey,
                PersistentDataType.BYTE
        );
    }

    private void registerRecipe() {

        ItemStack result = createHeavenlyTrident();

        NamespacedKey recipeKey =
                new NamespacedKey(this, "heavenly_trident_recipe");

        ShapedRecipe recipe =
                new ShapedRecipe(recipeKey, result);

        recipe.shape(
                "BBB",
                "DTD",
                "DGD"
        );

        recipe.setIngredient('B', Material.BEACON);
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('T', Material.TRIDENT);
        recipe.setIngredient('G', Material.GOLD_BLOCK);

        Bukkit.addRecipe(recipe);
    }

    /*
     * КРАФТ НЕБЕСНОГО ТРЕЗУБЦА
     *
     * При крафте показываем уведомление:
     * "Это Бог?"
     */
    @EventHandler
    public void onCraft(CraftItemEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();

        if (!isHeavenlyTrident(result)) {
            return;
        }

        Bukkit.getScheduler().runTask(
                this,
                () -> {

                    player.sendTitle(
                            "§b§lЭто Бог?",
                            "§fСоздай Небесный трезубец",
                            10,
                            50,
                            20
                    );

                    player.playSound(
                            player.getLocation(),
                            Sound.UI_TOAST_CHALLENGE_COMPLETE,
                            1.0f,
                            1.0f
                    );
                }
        );
    }

    /*
     * ПОПАДАНИЕ НЕБЕСНЫМ ТРЕЗУБЦОМ
     *
     * Обычный ванильный полёт трезубца.
     * Кастомного ItemDisplay-полёта больше нет.
     */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Trident trident)) {
            return;
        }

        ItemStack item = trident.getItemStack();

        if (!isHeavenlyTrident(item)) {
            return;
        }

        if (!(trident.getShooter() instanceof Player player)) {
            return;
        }

        // 15 урона
        event.setDamage(15.0);

        Location location = event.getEntity().getLocation();

        World world = location.getWorld();

        if (world == null) {
            return;
        }

        // Молния без дополнительного урона от самой молнии
        world.strikeLightningEffect(location);

        // Звук попадания
        world.playSound(
                location,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                1.0f,
                1.2f
        );
    }
}
