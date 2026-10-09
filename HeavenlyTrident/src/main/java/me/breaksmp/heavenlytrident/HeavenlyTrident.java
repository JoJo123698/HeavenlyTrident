
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
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HeavenlyTrident extends JavaPlugin implements Listener {

    private NamespacedKey tridentKey;
    private NamespacedKey recipeKey;

    private final Map<UUID, ItemDisplay> displays = new HashMap<>();
    private final Map<UUID, Trident> projectiles = new HashMap<>();

    @Override
    public void onEnable() {
        tridentKey = new NamespacedKey(this, "heavenly_trident");
        recipeKey = new NamespacedKey(this, "heavenly_trident_recipe");

        Bukkit.getPluginManager().registerEvents(this, this);
        createRecipe();

        getLogger().info("HeavenlyTrident enabled!");
    }

    @Override
    public void onDisable() {
        for (ItemDisplay display : displays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }

        displays.clear();
        projectiles.clear();
    }

    private ItemStack createHeavenlyTrident() {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName("§bНебесный трезубец");
        meta.setItemModel(new NamespacedKey("breaksmp", "trident_staff"));
        meta.setLore(List.of(
                "§7Оружие небес",
                "§cБазовый урон: 15",
                "§eМолния при попадании"
        ));

        meta.getPersistentDataContainer().set(
                tridentKey,
                PersistentDataType.BYTE,
                (byte) 1
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

        Byte value = meta.getPersistentDataContainer().get(
                tridentKey,
                PersistentDataType.BYTE
        );

        return value != null && value == (byte) 1;
    }

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

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!isHeavenlyTrident(event.getCurrentItem())) {
            return;
        }

        player.sendTitle(
                "§bЭто Бог?",
                "§fНебесный трезубец создан!",
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

        // Базовый урон 15; броня и зачарования работают обычно.
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

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) {
            return;
        }

        if (!(trident.getShooter() instanceof Player)) {
            return;
        }

        if (!isHeavenlyTrident(trident.getItemStack())) {
            return;
        }

        UUID id = trident.getUniqueId();

        // Скрываем ванильную модель у всех игроков.
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.hideEntity(this, trident);
        }

        projectiles.put(id, trident);

        ItemDisplay display = (ItemDisplay) trident.getWorld().spawnEntity(
                trident.getLocation(),
                EntityType.ITEM_DISPLAY
        );

        display.setItemStack(createHeavenlyTrident());
        display.setPersistent(false);
        display.setInterpolationDuration(1);
        display.setTeleportDuration(1);

        displays.put(id, display);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!trident.isValid() || trident.isDead()
                        || !display.isValid()) {
                    display.remove();
                    displays.remove(id);
                    projectiles.remove(id);
                    cancel();
                    return;
                }

                Location location = trident.getLocation();
                Vector velocity = trident.getVelocity();

                if (velocity.lengthSquared() > 0.0001) {
                    Vector direction = velocity.clone().normalize();

                    // Предполагается, что острие модели направлено по +Y.
                    Quaternionf rotation = new Quaternionf().rotationTo(
                            0.0f, 1.0f, 0.0f,
                            (float) direction.getX(),
                            (float) direction.getY(),
                            (float) direction.getZ()
                    );

                    display.setTransformation(new Transformation(
                            new Vector3f(0.0f, 0.0f, 0.0f),
                            rotation,
                            new Vector3f(1.0f, 1.0f, 1.0f),
                            new Quaternionf()
                    ));
                }

                display.teleport(location);
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        for (Trident trident : projectiles.values()) {
            if (trident != null && trident.isValid()) {
                player.hideEntity(this, trident);
            }
        }
    }
}
