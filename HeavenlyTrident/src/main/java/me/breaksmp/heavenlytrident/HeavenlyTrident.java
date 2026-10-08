package me.breaksmp.heavenlytrident;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
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

        /*
         * Метка нашего трезубца
         */

        meta.getPersistentDataContainer().set(
                tridentKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        /*
         * Кастомная 3D модель
         */

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
     * ПРОВЕРКА:
     * ЯВЛЯЕТСЯ ЛИ ПРЕДМЕТ НЕБЕСНЫМ ТРЕЗУБЦЕМ
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
     * БРОСОК ТРЕЗУБЦА
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

        /*
         * Проверяем, что это именно
         * Небесный трезубец.
         */

        if (
                !isHeavenlyTrident(
                        trident.getItemStack()
                )
        ) {
            return;
        }

        /*
         * Создаём визуальную модель.
         */

        createFlyingModel(
                trident,
                player
        );
    }

    /*
     * КАСТОМНАЯ МОДЕЛЬ В ПОЛЁТЕ
     */

    private void createFlyingModel(
            Trident trident,
            Player player
    ) {

        World world =
                trident.getWorld();

        /*
         * Прячем настоящий трезубец.
         *
         * Он всё ещё существует,
         * летит и регистрирует попадания.
         */

        trident.setInvisible(true);

        /*
         * Создаём ItemDisplay
         */

        ItemDisplay display =
                (ItemDisplay) world.spawnEntity(
                        trident.getLocation(),
                        EntityType.ITEM_DISPLAY
                );

        /*
         * Даём ему нашу модель
         */

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

        /*
         * Следим за настоящим трезубцем.
         */

        Bukkit.getScheduler().runTaskTimer(
                this,

                task -> {

                    /*
                     * Трезубец исчез / застрял /
                     * был удалён.
                     */

                    if (
                            !trident.isValid()
                    ) {

                        display.remove();

                        task.cancel();

                        return;
                    }

                    /*
                     * Защита от удаления модели.
                     */

                    if (
                            !display.isValid()
                    ) {

                        task.cancel();

                        return;
                    }

                    Location location =
                            trident.getLocation();

                    /*
                     * Перемещаем модель
                     * на позицию трезубца.
                     */

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
                         * Наша модель вытянута
                         * вдоль X.
                         *
                         * Поэтому направляем
                         * ось X по траектории.
                         */

                        org.joml.Vector3f from =
                                new org.joml.Vector3f(
                                        1,
                                        0,
                                        0
                                );

                        org.joml.Vector3f to =
                                new org.joml.Vector3f(
                                        (float)
                                                direction.getX(),
                                        (float)
                                                direction.getY(),
                                        (float)
                                                direction.getZ()
                                );

                        org.joml.Quaternionf rotation =
                                new org.joml.Quaternionf()
                                        .rotationTo(
                                                from,
                                                to
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
     * ПОПАДАНИЕ
     *
     * 15 УРОНА + МОЛНИЯ
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

        /*
         * Только Небесный трезубец
         */

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
         * МОЛНИЯ
         *
         * strikeLightningEffect
         * показывает молнию,
         * но сама молния не наносит
         * дополнительный урон.
         */

        world.strikeLightningEffect(
                location
        );

        /*
         * Звук грома
         */

        world.playSound(
                location,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                1.0f,
                1.2f
        );
    }
}
