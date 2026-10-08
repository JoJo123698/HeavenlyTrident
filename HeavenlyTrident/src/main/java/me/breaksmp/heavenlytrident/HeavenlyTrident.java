package me.breaksmp.heavenlytrident;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public final class HeavenlyTrident extends JavaPlugin implements Listener {

    private NamespacedKey tridentKey;

    @Override
    public void onEnable() {

        tridentKey = new NamespacedKey(
                this,
                "heavenly_trident"
        );

        Bukkit.getPluginManager().registerEvents(
                this,
                this
        );

        registerRecipe();

        getLogger().info(
                "HeavenlyTrident enabled!"
        );
    }

    /*
     * СОЗДАНИЕ НЕБЕСНОГО ТРЕЗУБЦА
     */

    public ItemStack createHeavenlyTrident() {

        ItemStack item =
                new ItemStack(Material.TRIDENT);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                net.kyori.adventure.text.Component
                        .text("Небесный трезубец")
                        .color(
                                net.kyori.adventure.text.format
                                        .NamedTextColor.AQUA
                        )
                        .decoration(
                                net.kyori.adventure.text.format
                                        .TextDecoration.ITALIC,
                                false
                        )
        );

        meta.getPersistentDataContainer().set(
                tridentKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.setItemModel(
                new NamespacedKey(
                        "breaksmp",
                        "trident_staff"
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    /*
     * ПРОВЕРКА НА НЕБЕСНЫЙ ТРЕЗУБЕЦ
     */

    private boolean isHeavenlyTrident(
            ItemStack item
    ) {

        if (
                item == null ||
                item.getType() != Material.TRIDENT
        ) {
            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        return meta
                .getPersistentDataContainer()
                .has(
                        tridentKey,
                        PersistentDataType.BYTE
                );
    }

    /*
     * РЕЦЕПТ
     *
     * BBB
     * DTD
     * DGD
     */

    private void registerRecipe() {

        ItemStack result =
                createHeavenlyTrident();

        NamespacedKey recipeKey =
                new NamespacedKey(
                        this,
                        "heavenly_trident_recipe"
                );

        ShapedRecipe recipe =
                new ShapedRecipe(
                        recipeKey,
                        result
                );

        recipe.shape(
                "BBB",
                "DTD",
                "DGD"
        );

        recipe.setIngredient(
                'B',
                Material.BEACON
        );

        recipe.setIngredient(
                'D',
                Material.DIAMOND_BLOCK
        );

        recipe.setIngredient(
                'T',
                Material.TRIDENT
        );

        recipe.setIngredient(
                'G',
                Material.GOLD_BLOCK
        );

        Bukkit.addRecipe(recipe);
    }

    /*
     * КРАФТ
     *
     * "Это Бог?"
     */

    @EventHandler
    public void onCraft(
            CraftItemEvent event
    ) {

        if (
                !(event.getWhoClicked()
                        instanceof Player player)
        ) {
            return;
        }

        ItemStack result =
                event.getRecipe().getResult();

        if (
                !isHeavenlyTrident(result)
        ) {
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
     * БРОСОК
     */

    @EventHandler
    public void onProjectileLaunch(
            ProjectileLaunchEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof Trident trident)
        ) {
            return;
        }

        if (
                !(trident.getShooter()
                        instanceof Player player)
        ) {
            return;
        }

        if (
                !isHeavenlyTrident(
                        trident.getItemStack()
                )
        ) {
            return;
        }

        /*
         * Скрываем настоящий
         * ванильный трезубец.
         */

        for (Player onlinePlayer :
                Bukkit.getOnlinePlayers()) {

            onlinePlayer.hideEntity(
                    this,
                    trident
            );
        }

        /*
         * Создаём нашу модель.
         */

        createFlyingModel(
                trident
        );
    }

    /*
     * КАСТОМНАЯ 3D МОДЕЛЬ В ПОЛЁТЕ
     */

    private void createFlyingModel(
            Trident trident
    ) {

        World world =
                trident.getWorld();

        ItemDisplay display =
                (ItemDisplay) world.spawnEntity(
                        trident.getLocation(),
                        EntityType.ITEM_DISPLAY
                );

        display.setItemStack(
                createHeavenlyTrident()
        );

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

        display.setTransformation(
                transformation
        );

        Bukkit.getScheduler().runTaskTimer(
                this,

                task -> {

                    /*
                     * Настоящий трезубец исчез.
                     */

                    if (
                            !trident.isValid()
                    ) {

                        display.remove();

                        task.cancel();

                        return;
                    }

                    /*
                     * Позиция модели
                     */

                    Location location =
                            trident.getLocation();

                    display.teleport(
                            location
                    );

                    /*
                     * Направление полёта
                     */

                    Vector velocity =
                            trident.getVelocity();

                    if (
                            velocity.lengthSquared()
                                    > 0.001
                    ) {

                        Vector direction =
                                velocity.clone()
                                        .normalize();

                        /*
                         * Вычисляем направление
                         * по горизонтали.
                         */

                        float yaw =
                                (float) Math.toDegrees(
                                        Math.atan2(
                                                -direction.getX(),
                                                direction.getZ()
                                        )
                                );

                        /*
                         * Вычисляем вертикальный
                         * наклон.
                         */

                        float pitch =
                                (float) Math.toDegrees(
                                        Math.asin(
                                                -direction.getY()
                                        )
                                );

                        /*
                         * Поворот модели.
                         *
                         * ItemDisplay использует
                         * направление -Z.
                         */

                        org.joml.Quaternionf rotation =
                                new org.joml.Quaternionf()
                                        .rotateY(
                                                (float) Math.toRadians(
                                                        yaw
                                                )
                                        )
                                        .rotateX(
                                                (float) Math.toRadians(
                                                        pitch
                                                )
                                        );

                        Transformation transform =
                                display.getTransformation();

                        transform
                                .getLeftRotation()
                                .set(rotation);

                        display.setTransformation(
                                transform
                        );
                    }

                },

                1L,
                1L
        );
    }

    /*
     * УРОН + МОЛНИЯ
     */

    @EventHandler
    public void onDamage(
            EntityDamageByEntityEvent event
    ) {

        if (
                !(event.getDamager()
                        instanceof Trident trident)
        ) {
            return;
        }

        if (
                !isHeavenlyTrident(
                        trident.getItemStack()
                )
        ) {
            return;
        }

        if (
                !(trident.getShooter()
                        instanceof Player player)
        ) {
            return;
        }

        /*
         * 15 УРОНА
         */

        event.setDamage(15.0);

        Location location =
                event.getEntity()
                        .getLocation();

        World world =
                location.getWorld();

        if (world == null) {
            return;
        }

        /*
         * МОЛНИЯ БЕЗ ДОПОЛНИТЕЛЬНОГО УРОНА
         */

        world.strikeLightningEffect(
                location
        );

        world.playSound(
                location,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                1.0f,
                1.2f
        );
    }

    /*
     * ЕСЛИ ИГРОК ЗАЙДЁТ ВО ВРЕМЯ ПОЛЁТА,
     * СКРЫВАЕМ ОТ НЕГО ТРЕЗУБЕЦ.
     */

    @EventHandler
    public void onJoin(
            PlayerJoinEvent event
    ) {

        Player player =
                event.getPlayer();

        for (org.bukkit.entity.Entity entity :
                player.getWorld().getEntitiesByClass(
                        Trident.class
                )) {

            if (
                    entity instanceof Trident trident &&
                    isHeavenlyTrident(
                            trident.getItemStack()
                    )
            ) {

                player.hideEntity(
                        this,
                        trident
                );
            }
        }
    }
}
