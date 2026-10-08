package me.breaksmp.heavenlytrident;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class HeavenlyTrident extends JavaPlugin implements Listener {

    private NamespacedKey tridentKey;

    @Override
    public void onEnable() {

        tridentKey = new NamespacedKey(this, "heavenly_trident");

        Bukkit.getPluginManager().registerEvents(this, this);

        registerRecipe();

        getLogger().info("HeavenlyTrident enabled!");
    }

    // =========================================================
    // КАСТОМНЫЙ ПРЕДМЕТ
    // =========================================================

    public ItemStack createHeavenlyTrident() {

        ItemStack item = new ItemStack(Material.TRIDENT);

        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                net.kyori.adventure.text.Component.text("Небесный трезубец")
                        .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
        );

        meta.getPersistentDataContainer().set(
                tridentKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        // Наша модель из ресурс-пака
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

    // =========================================================
    // КРАФТ
    // =========================================================

    private void registerRecipe() {

        ItemStack result = createHeavenlyTrident();

        NamespacedKey recipeKey =
                new NamespacedKey(this, "heavenly_trident_recipe");

        ShapedRecipe recipe =
                new ShapedRecipe(recipeKey, result);

        /*
         * Маяк | Маяк | Маяк
         * Алмазный блок | Трезубец | Алмазный блок
         * Алмазный блок | Золотой блок | Алмазный блок
         */

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

    // =========================================================
    // БРОСОК
    // =========================================================

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {

        if (!(event.getEntity() instanceof org.bukkit.entity.Trident trident)) {
            return;
        }

        if (!(trident.getShooter() instanceof Player player)) {
            return;
        }

        ItemStack item = trident.getItemStack();

        if (!isHeavenlyTrident(item)) {
            return;
        }

        /*
         * Отменяем настоящий vanilla-трезубец.
         * Вместо него создаём наш ItemDisplay.
         */

        event.setCancelled(true);

        Location location = trident.getLocation();

        Vector velocity = trident.getVelocity();

        spawnCustomProjectile(player, location, velocity);
    }

    // =========================================================
    // СОЗДАНИЕ ЛЕТЯЩЕЙ МОДЕЛИ
    // =========================================================

    private void spawnCustomProjectile(
            Player player,
            Location location,
            Vector velocity
    ) {

        World world = location.getWorld();

        if (world == null) {
            return;
        }

        ItemDisplay display =
                (ItemDisplay) world.spawnEntity(
                        location,
                        EntityType.ITEM_DISPLAY
                );

        ItemStack displayItem = createHeavenlyTrident();

        display.setItemStack(displayItem);

        display.setBillboard(
                org.bukkit.entity.Display.Billboard.FIXED
        );

        display.setInterpolationDuration(1);

        display.setTeleportDuration(1);

        /*
         * Размер модели
         */

        Transformation transformation =
                display.getTransformation();

        transformation.getScale().set(
                0.75f,
                0.75f,
                0.75f
        );

        display.setTransformation(transformation);

        /*
         * Запускаем собственную физику
         */

        final Vector currentVelocity =
                velocity.clone();

        final int[] ticks = {0};

        Bukkit.getScheduler().runTaskTimer(
                this,
                task -> {

                    if (!display.isValid()) {
                        task.cancel();
                        return;
                    }

                    ticks[0]++;

                    Location oldLocation =
                            display.getLocation();

                    Location newLocation =
                            oldLocation.clone().add(currentVelocity);

                    /*
                     * Проверяем столкновение
                     */

                    if (newLocation.getBlock().getType().isSolid()) {

                        world.spawnParticle(
                                Particle.ELECTRIC_SPARK,
                                oldLocation,
                                20,
                                0.25,
                                0.25,
                                0.25,
                                0.05
                        );

                        world.playSound(
                                oldLocation,
                                Sound.BLOCK_AMETHYST_BLOCK_HIT,
                                1.0f,
                                1.2f
                        );

                        display.remove();
                        task.cancel();
                        return;
                    }

                    /*
                     * Проверяем игроков/мобов
                     */

                    for (Entity entity :
                            world.getNearbyEntities(
                                    newLocation,
                                    0.7,
                                    0.7,
                                    0.7
                            )) {

                        if (entity == player) {
                            continue;
                        }

                        if (entity instanceof ItemDisplay) {
                            continue;
                        }

                        if (!entity.getType().isAlive()) {
                            continue;
                        }

                        /*
                         * Урон
                         */

                        if (entity instanceof org.bukkit.entity.Damageable damageable) {

                            damageable.damage(
                                    12.0,
                                    player
                            );

                            world.spawnParticle(
                                    Particle.CRIT,
                                    newLocation,
                                    15,
                                    0.2,
                                    0.2,
                                    0.2,
                                    0.1
                            );

                            world.playSound(
                                    newLocation,
                                    Sound.ITEM_TRIDENT_HIT,
                                    1.0f,
                                    1.0f
                            );

                            display.remove();
                            task.cancel();
                            return;
                        }
                    }

                    /*
                     * Перемещаем модель
                     */

                    display.teleport(newLocation);

                    /*
                     * Поворачиваем трезубец по направлению полёта
                     */

                    Vector direction =
                            currentVelocity.clone().normalize();

                    float yaw =
                            (float) Math.atan2(
                                    direction.getZ(),
                                    direction.getX()
                            );

                    float pitch =
                            (float) Math.asin(
                                    direction.getY()
                            );

                    Transformation transform =
                            display.getTransformation();

                    transform.getLeftRotation().set(
                            new AxisAngle4f(
                                    -pitch,
                                    0,
                                    1,
                                    0
                            )
                    );

                    display.setTransformation(transform);

                    /*
                     * Гравитация
                     */

                    currentVelocity.setY(
                            currentVelocity.getY() - 0.035
                    );

                    /*
                     * Небольшое сопротивление воздуха
                     */

                    currentVelocity.multiply(0.99);

                    /*
                     * Частицы за трезубцем
                     */

                    if (ticks[0] % 2 == 0) {

                        world.spawnParticle(
                                Particle.END_ROD,
                                newLocation,
                                1,
                                0,
                                0,
                                0,
                                0
                        );
                    }

                    /*
                     * Максимальное время полёта
                     */

                    if (ticks[0] >= 200) {

                        display.remove();
                        task.cancel();
                    }

                },
                1L,
                1L
        );
    }
}