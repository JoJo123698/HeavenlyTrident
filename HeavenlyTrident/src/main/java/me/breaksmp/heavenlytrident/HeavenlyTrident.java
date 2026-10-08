private void createFlyingModel(
        Trident trident
) {

    World world = trident.getWorld();

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
                 * Если настоящий трезубец исчез —
                 * удаляем модель.
                 */
                if (!trident.isValid()) {

                    display.remove();
                    task.cancel();

                    return;
                }

                /*
                 * Перемещаем модель за настоящим
                 * трезубцем.
                 */
                Location location =
                        trident.getLocation();

                display.teleport(location);

                /*
                 * Направление полёта
                 */
                Vector velocity =
                        trident.getVelocity();

                if (velocity.lengthSquared() > 0.001) {

                    Vector direction =
                            velocity.clone().normalize();

                    /*
                     * ВАЖНО:
                     *
                     * Теперь считаем, что древко
                     * модели направлено по Y.
                     *
                     * Поэтому поворачиваем
                     * ось Y в направление полёта.
                     */
                    org.joml.Vector3f from =
                            new org.joml.Vector3f(
                                    0,
                                    1,
                                    0
                            );

                    org.joml.Vector3f to =
                            new org.joml.Vector3f(
                                    (float) direction.getX(),
                                    (float) direction.getY(),
                                    (float) direction.getZ()
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
