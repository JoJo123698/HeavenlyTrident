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

                if (!trident.isValid()) {

                    display.remove();
                    task.cancel();

                    return;
                }

                /*
                 * Позиция
                 */

                Location location =
                        trident.getLocation();

                display.teleport(location);

                /*
                 * Получаем направление
                 * самого трезубца.
                 */

                Vector velocity =
                        trident.getVelocity();

                if (velocity.lengthSquared() > 0.001) {

                    Vector direction =
                            velocity.clone().normalize();

                    /*
                     * Получаем yaw и pitch
                     * направления полёта.
                     */

                    float yaw =
                            (float) Math.toDegrees(
                                    Math.atan2(
                                            -direction.getX(),
                                            direction.getZ()
                                    )
                            );

                    float pitch =
                            (float) Math.toDegrees(
                                    Math.asin(
                                            -direction.getY()
                                    )
                            );

                    /*
                     * Создаём поворот.
                     *
                     * Minecraft-модель ItemDisplay
                     * смотрит вперёд по -Z.
                     */

                    org.joml.Quaternionf rotation =
                            new org.joml.Quaternionf()
                                    .rotateY(
                                            (float) Math.toRadians(yaw)
                                    )
                                    .rotateX(
                                            (float) Math.toRadians(pitch)
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
