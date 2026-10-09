package me.breaksmp.heavenlytrident;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HeavenlyTrident extends JavaPlugin implements Listener {

    private NamespacedKey tridentKey;
    private NamespacedKey modelKey;
    private NamespacedKey recipeKey;

    private final Map<UUID, Trident> projectiles = new HashMap<>();
    private final Map<UUID, ItemDisplay> displays = new HashMap<>();

    @Override
    public void onEnable() {
        tridentKey = new NamespacedKey(this, "heavenly_trident");
        modelKey = new NamespacedKey("breaksmp", "trident_staff");
        recipeKey = new NamespacedKey(this, "heavenly_trident_recipe");

        Bukkit.getPluginManager().registerEvents(this, this);

        createRecipe();
        startProjectileTask();

        getLogger().info("Небесный трезубец включён!");
    }

    @Override
    public void onDisable() {
        for (ItemDisplay display : displays.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }

        displays.clear();
        projectiles.clear();
    }

    // Создание предмета
    public ItemStack createHeavenlyTrident() {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName("§bНебесный трезубец");
        meta.setItemModel(modelKey);

        meta.setLore(java.util.Arrays.asList(
                "§7Оружие небес",
                "§7Базовый урон: §c15",
                "§7Молния при попадании"
        ));

        meta.getPersistentDataContainer().set(
                tridentKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    // Проверка, является ли предмет нашим трезубцем
    private boolean isHeavenlyTrident(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        return meta != null
                && meta.getPersistentDataContainer().has(
                tridentKey,
                PersistentDataType.BYTE
        );
    }

    // Рецепт крафта
    private void createRecipe() {
        Bukkit.removeRecipe(recipeKey);

        ShapedRecipe recipe = new ShapedRecipe(
                recipeKey,
                createHeavenlyTrident()
        );

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

    // Эффект при создании трезубца
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!isHeavenlyTrident(event.getCurrentItem())) {
            return;
        }

        if (event.getWhoClicked() instanceof Player player) {
            player.sendTitle(
                    "§bЭто Бог?",
                    "§7Ты создал Небесный трезубец",
                    10,
                    50,
                    10
            );

            player.playSound(
                    player.getLocation(),
                    Sound.UI_TOAST_CHALLENGE_COMPLETE,
                    1.0f,
                    1.0f
            );
        }
    }

    // Настоящий снаряд сохраняет физику Minecraft
    @EventHandler
    public void onTridentThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) {
            return;
        }

        if (!isHeavenlyTrident(trident.getItemStack())) {
            return;
        }

        UUID id = trident.getUniqueId();

        projectiles.put(id, trident);

        // Прячем обычную модель снаряда у всех игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideEntity(this, trident);
        }

        // Создаём вместо неё нашу модель
        ItemDisplay display = trident.getWorld().spawn(
                trident.getLocation(),
                ItemDisplay.class
        );

        display.setItemStack(createHeavenlyTrident());
        display.setItemDisplayTransform(
                ItemDisplay.ItemDisplayTransform.FIXED
        );
        display.setPersistent(false);

        displays.put(id, display);

        // Сразу выставляем правильное направление
        rotateDisplayToVelocity(trident, display);
    }

    // Поворачиваем 3D-модель по направлению полёта
    private void rotateDisplayToVelocity(
            Trident trident,
            ItemDisplay display
    ) {
        var velocity = trident.getVelocity();

        double dx = velocity.getX();
        double dy = velocity.getY();
        double dz = velocity.getZ();

        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length < 0.0001) {
            return;
        }

        // Направление полёта, нормализованное до длины 1
        float dirX = (float) (dx / length);
        float dirY = (float) (dy / length);
        float dirZ = (float) (dz / length);

        // Считаем поворот: локальная ось +Z модели
        // направляется точно по вектору движения снаряда
        Quaternionf rotation = new Quaternionf().rotationTo(
                0.0f, 0.0f, 1.0f,
                dirX, dirY, dirZ
        );

        display.setTransformation(new Transformation(
                new Vector3f(0.0f, 0.0f, 0.0f),
                rotation,
                new Vector3f(1.0f, 1.0f, 1.0f),
                new Quaternionf()
        ));
    }

    // Обновляем позицию и направление модели каждый тик
    private void startProjectileTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID id : projectiles.keySet().toArray(new UUID[0])) {
                    Trident trident = projectiles.get(id);
                    ItemDisplay display = displays.get(id);

                    if (trident == null
                            || !trident.isValid()
                            || trident.isDead()
                            || display == null
                            || !display.isValid()) {

                        if (display != null && display.isValid()) {
                            display.remove();
                        }

                        displays.remove(id);
                        projectiles.remove(id);
                        continue;
                    }

                    // Переносим модель в положение снаряда
                    Location location = trident.getLocation();
                    display.teleport(location);

                    // Поворачиваем модель вслед за траекторией
                    rotateDisplayToVelocity(trident, display);
                }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    // Прячем обычную модель и у игроков, которые вошли позже
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        for (Trident trident : projectiles.values()) {
            if (trident != null && trident.isValid()) {
                player.hideEntity(this, trident);
            }
        }
    }

    // Урон 15 с обычным учётом брони и зачарований
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Trident trident)) {
            return;
        }

        if (!isHeavenlyTrident(trident.getItemStack())) {
            return;
        }

        if (!(trident.getShooter() instanceof Player)) {
            return;
        }

        event.setDamage(15.0);

        Location location = event.getEntity().getLocation();
        World world = location.getWorld();

        if (world != null) {
            world.strikeLightningEffect(location);

            world.playSound(
                    location,
                    Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                    1.0f,
                    1.2f
            );
        }
    }
}
