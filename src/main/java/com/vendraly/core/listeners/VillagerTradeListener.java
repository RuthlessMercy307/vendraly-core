package com.vendraly.core.listeners;

import com.vendraly.core.Main;
import com.vendraly.core.economy.CashManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerTradeListener implements Listener {

    private final Main plugin;
    private final CashManager cashManager;
    private static final int CUSTOM_MODEL_DATA = 9000;
    private static final double PRICE_MULTIPLIER = 8.0;
    private static final int MAX_USES_CAP = 12;
    private final Map<UUID, Object> locks = new ConcurrentHashMap<>();

    public VillagerTradeListener(Main plugin) {
        this.plugin = plugin;
        this.cashManager = plugin.getCashManager();
    }

    private Object getLockForVillager(UUID id) {
        return locks.computeIfAbsent(id, k -> new Object());
    }

    private boolean isMendingBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }

        if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            return meta.hasStoredEnchant(Enchantment.MENDING);
        }
        return false;
    }

    private ItemStack createCashStack(int amount) {
        ItemStack item = new ItemStack(Material.EMERALD, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Efectivo Requerido")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Representa el costo de:").color(NamedTextColor.GRAY));
            lore.add(Component.text(String.format("$%.2f", (double) amount)).color(NamedTextColor.GREEN));

            meta.lore(lore);
            meta.setCustomModelData(CUSTOM_MODEL_DATA);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (!(event.getInventory().getHolder() instanceof Villager villager)) return;

        // Forzar el nivel 5 (Maestro) y la experiencia máxima.
        villager.setVillagerLevel(5);
        villager.setVillagerExperience(Integer.MAX_VALUE);

        new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (getLockForVillager(villager.getUniqueId())) {
                    try {
                        List<MerchantRecipe> original = new ArrayList<>(villager.getRecipes());
                        List<MerchantRecipe> newRecipes = new ArrayList<>(original.size());
                        boolean modified = false;

                        for (MerchantRecipe r : original) {
                            // Omitir recetas que resultan en libros de Reparación
                            if (isMendingBook(r.getResult())) {
                                plugin.getLogger().fine("Receta de Mending eliminada para el aldeano: " + villager.getName());
                                continue;
                            }

                            MerchantRecipe conv = convertRecipeToCash(r);
                            if (conv != null) {
                                newRecipes.add(conv);
                                modified = true;
                            } else {
                                newRecipes.add(r);
                            }
                        }

                        if (modified) {
                            villager.setRecipes(newRecipes);
                            plugin.getLogger().fine("Recetas del aldeano actualizadas al abrir inventario - total: " + newRecipes.size());
                        }

                        // Actualizar DisplayName
                        updateVillagerDisplayName(villager);

                    } catch (Exception e) {
                        plugin.getLogger().warning("Error en onInventoryOpen: " + e.getMessage());
                    }
                }
            }
        }.runTaskLater(plugin, 1L); // 1 tick de retraso para asegurar que la GUI se cargue
    }

    private MerchantRecipe getRecipeFromEvent(TradeSelectEvent event) {
        if (!(event.getInventory().getHolder() instanceof Merchant)) return null;
        Merchant merchant = (Merchant) event.getInventory().getHolder();
        List<MerchantRecipe> recipes = merchant.getRecipes();
        if (recipes == null || recipes.isEmpty()) return null;
        int idx = event.getIndex();

        if (idx < 0 || idx >= recipes.size()) return null;

        return recipes.get(idx);
    }

    @EventHandler
    public void onTradeSelect(TradeSelectEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Debes iniciar sesión para tradear con aldeanos.")
                    .color(NamedTextColor.RED));
            return;
        }

        // 1. Cancelamos inmediatamente para tomar el control del tradeo.
        event.setCancelled(true);

        MerchantRecipe recipe = getRecipeFromEvent(event);

        if (recipe == null) {
            player.sendMessage(Component.text("Receta no encontrada.").color(NamedTextColor.RED));
            return;
        }

        // BLOQUEO DE MENDING
        if (isMendingBook(recipe.getResult())) {
            player.sendMessage(Component.text("§c[Vendraly] El libro de Reparación (Mending) está prohibido.")
                    .decorate(TextDecoration.BOLD));
            return;
        }

        if (!(event.getInventory().getHolder() instanceof Villager villager)) {
            player.sendMessage(Component.text("Error: Holder no es un aldeano.").color(NamedTextColor.RED));
            return;
        }

        final double requiredCash = calculateRequiredCash(recipe);

        // Verifica si el jugador tiene los items de INGREDIENTE (excepto el cash)
        if (!playerHasRequiredItems(player, recipe)) {
            player.sendMessage(Component.text("No tienes todos los items necesarios para el tradeo.").color(NamedTextColor.RED));
            return;
        }

        try {
            if (requiredCash <= 0.0) {
                // Si el costo es 0 (no se encontró CashStack), aplicamos un costo base.
                applyBaseCostToTrade(villager, event, recipe, player);
                return;
            }

            processTradeWithCash(villager, event, recipe, requiredCash, player);
        } catch (Exception e) {
            plugin.getLogger().warning("Error en onTradeSelect para " + player.getName() + ": " + e.getMessage());
            player.sendMessage(Component.text("Error interno procesando el trade.")
                    .color(NamedTextColor.RED));
        }
    }

    private boolean playerHasRequiredItems(Player player, MerchantRecipe recipe) {
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient == null) continue;

            ItemMeta meta = ingredient.getItemMeta();
            boolean isCustomCash = ingredient.getType() == Material.EMERALD && meta != null
                    && meta.hasCustomModelData() && meta.getCustomModelData() == CUSTOM_MODEL_DATA;

            // Si es la esmeralda de cash, la ignoramos.
            if (isCustomCash) continue;

            // Verificamos si el jugador tiene el item de ingrediente.
            if (!player.getInventory().containsAtLeast(ingredient, ingredient.getAmount())) {
                return false;
            }
        }
        return true;
    }

    private boolean consumeIngredients(Player player, MerchantRecipe recipe) {
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient == null) continue;

            ItemMeta meta = ingredient.getItemMeta();
            boolean isCustomCash = ingredient.getType() == Material.EMERALD && meta != null
                    && meta.hasCustomModelData() && meta.getCustomModelData() == CUSTOM_MODEL_DATA;

            // Si es la esmeralda de cash, la ignoramos.
            if (isCustomCash) continue;

            // Consumimos el ingrediente normal.
            player.getInventory().removeItem(ingredient);
        }
        return true;
    }


    private void applyBaseCostToTrade(Villager villager, TradeSelectEvent event, MerchantRecipe recipe, Player player) {
        final int baseCost = (int) Math.ceil(5 * PRICE_MULTIPLIER);
        cashManager.getCash(player.getUniqueId()).thenAccept(currentCash -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                if (currentCash >= baseCost) {
                    cashManager.modifyBalance(player.getUniqueId(), -baseCost).thenAccept(success ->
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (!player.isOnline()) return;

                                if (success) {
                                    // 1. CONSUMIR INGREDIENTES (CRÍTICO)
                                    consumeIngredients(player, recipe);

                                    // 2. DAR RESULTADO
                                    applyTrade(player, recipe);

                                    // 3. SIMULAR USO
                                    incrementTradeUsesAndExperience(villager, recipe);

                                    player.sendMessage(Component.text("Trade realizado. Se cobró ")
                                            .color(NamedTextColor.GREEN)
                                            .append(Component.text(String.format("$%.2f", (double) baseCost)).color(NamedTextColor.GOLD))
                                            .append(Component.text(" de tu efectivo.").color(NamedTextColor.GREEN)));

                                    updatePlayerUI(player);
                                } else {
                                    player.sendMessage(Component.text("Error interno al procesar el cobro.").color(NamedTextColor.RED));
                                }
                            })
                    ).exceptionally(throwable -> {
                        plugin.getLogger().warning("Error modificando balance: " + throwable.getMessage());
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Error al procesar el pago.").color(NamedTextColor.RED));
                            }
                        });
                        return null;
                    });
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) return;

                        player.sendMessage(Component.text("Fondos insuficientes. Necesitas ")
                                .color(NamedTextColor.RED)
                                .append(Component.text(String.format("$%.2f", (double) baseCost)).color(NamedTextColor.GOLD))
                                .append(Component.text(" en efectivo.").color(NamedTextColor.RED)));
                    });
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().warning("Error al obtener cash: " + throwable.getMessage());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(Component.text("Error al consultar tu saldo.").color(NamedTextColor.RED));
                }
            });
            return null;
        });
    }


    private double calculateRequiredCash(MerchantRecipe recipe) {
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient != null && ingredient.getType() == Material.EMERALD) {
                ItemMeta meta = ingredient.getItemMeta();
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == CUSTOM_MODEL_DATA) {
                    return ingredient.getAmount();
                }
            }
        }
        return 0.0;
    }

    private void processTradeWithCash(Villager villager, TradeSelectEvent event, MerchantRecipe recipe, double requiredCash, Player player) {
        // La comprobación de Mending se ha movido al onTradeSelect.

        cashManager.getCash(player.getUniqueId()).thenAccept(currentCash -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                if (currentCash >= requiredCash) {
                    cashManager.modifyBalance(player.getUniqueId(), -requiredCash).thenAccept(success ->
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (!player.isOnline()) return;

                                if (success) {
                                    // 1. CONSUMIR INGREDIENTES (CRÍTICO)
                                    consumeIngredients(player, recipe);

                                    // 2. DAR RESULTADO
                                    applyTrade(player, recipe);

                                    // 3. SIMULAR USO
                                    incrementTradeUsesAndExperience(villager, recipe);

                                    player.sendMessage(Component.text("Trade realizado. Se cobró ")
                                            .color(NamedTextColor.GREEN)
                                            .append(Component.text(String.format("$%.2f", requiredCash)).color(NamedTextColor.GOLD))
                                            .append(Component.text(" de tu efectivo.").color(NamedTextColor.GREEN)));

                                    updatePlayerUI(player);
                                } else {
                                    player.sendMessage(Component.text("Error interno al procesar el cobro.").color(NamedTextColor.RED));
                                }
                            })
                    ).exceptionally(throwable -> {
                        plugin.getLogger().warning("Error al modificar balance para " + player.getName() + ": " + throwable.getMessage());
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Error al procesar el pago.").color(NamedTextColor.RED));
                            }
                        });
                        return null;
                    });
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) return;

                        player.sendMessage(Component.text("Fondos insuficientes. Necesitas ")
                                .color(NamedTextColor.RED)
                                .append(Component.text(String.format("$%.2f", requiredCash)).color(NamedTextColor.GOLD))
                                .append(Component.text(" en efectivo, pero solo tienes ").color(NamedTextColor.RED))
                                .append(Component.text(String.format("$%.2f", currentCash)).color(NamedTextColor.GOLD))
                                .append(Component.text(".").color(NamedTextColor.RED)));
                    });
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().warning("Error al obtener cash para " + player.getName() + ": " + throwable.getMessage());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(Component.text("Error al consultar tu saldo.").color(NamedTextColor.RED));
                }
            });
            return null;
        });
    }

    private boolean incrementTradeUsesAndExperience(Villager villager, MerchantRecipe recipe) {
        UUID vid = villager.getUniqueId();

        synchronized (getLockForVillager(vid)) {
            try {
                // Iteramos sobre las recetas para encontrar la que coincide y actualizarla.
                List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
                boolean found = false;

                for (int i = 0; i < recipes.size(); i++) {
                    MerchantRecipe currentRecipe = recipes.get(i);
                    // Comprobamos si las recetas son iguales (ingredientes y resultado)
                    if (currentRecipe.equals(recipe)) {
                        int currentUses = currentRecipe.getUses();
                        currentRecipe.setUses(currentUses + 1);

                        int newMaxUses = getAdjustedMaxUses(currentRecipe.getMaxUses());
                        currentRecipe.setMaxUses(newMaxUses);

                        villager.setRecipes(recipes); // Forzar la actualización de la GUI
                        found = true;
                        break;
                    }
                }

                if (found) {
                    int experienceToAdd = recipe.getVillagerExperience();
                    if (villager.getVillagerLevel() < 5 && experienceToAdd > 0) {
                        villager.setVillagerExperience(villager.getVillagerExperience() + experienceToAdd);
                    }
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error al incrementar usos y experiencia: " + e.getMessage());
            }
            return false;
        }
    }


    private MerchantRecipe convertRecipeToCash(MerchantRecipe originalRecipe) {
        try {
            // Eliminar Mending al convertir
            if (isMendingBook(originalRecipe.getResult())) {
                return null;
            }

            List<ItemStack> newIngredients = new ArrayList<>();
            int totalEmeraldsValue = 0;
            boolean conversionHappened = false;

            for (ItemStack ingredient : originalRecipe.getIngredients()) {
                if (ingredient == null) continue;

                ItemMeta meta = ingredient.getItemMeta();
                boolean isCustomCash = ingredient.getType() == Material.EMERALD && meta != null
                        && meta.hasCustomModelData() && meta.getCustomModelData() == CUSTOM_MODEL_DATA;

                if (isCustomCash) {
                    // Si ya está convertido, devolvemos null para mantener la original.
                    return null;
                }

                if (ingredient.getType() == Material.EMERALD) {
                    // Si es una esmeralda normal, convertir su valor
                    int adjustedAmount = Math.max(1, (int) Math.ceil(ingredient.getAmount() * PRICE_MULTIPLIER));
                    newIngredients.add(createCashStack(adjustedAmount));
                    totalEmeraldsValue += adjustedAmount;
                    conversionHappened = true;
                } else {
                    // Si no es esmeralda, mantener el ingrediente original
                    newIngredients.add(ingredient);
                }
            }

            // Si no se convirtió ninguna esmeralda Y no tiene ingredientes, aplicamos el costo base
            if (!conversionHappened && originalRecipe.getIngredients().isEmpty()) {
                int baseCost = (int) Math.ceil(5 * PRICE_MULTIPLIER);
                newIngredients.add(createCashStack(baseCost));
                totalEmeraldsValue = baseCost;
                conversionHappened = true;
            } else if (!conversionHappened) {
                // Si la conversión no ocurrió y ya tenía ingredientes (no Emerald), mantenemos la original
                return null;
            }

            MerchantRecipe newRecipe = new MerchantRecipe(
                    originalRecipe.getResult() == null ? new ItemStack(Material.AIR) : originalRecipe.getResult(),
                    originalRecipe.getUses(),
                    getAdjustedMaxUses(originalRecipe.getMaxUses()),
                    true,
                    getAdjustedExperience(originalRecipe.getVillagerExperience(), totalEmeraldsValue),
                    0.0f
            );

            try {
                newRecipe.setDemand(0);
                newRecipe.setSpecialPrice(0);
            } catch (NoSuchMethodError ignored) {}

            List<ItemStack> finalIngredients = new ArrayList<>();
            for(ItemStack ing : newIngredients) {
                if (ing != null) {
                    finalIngredients.add(ing);
                }
            }
            newRecipe.setIngredients(finalIngredients);
            return newRecipe;

        } catch (Exception e) {
            plugin.getLogger().warning("Error en convertRecipeToCash: " + e.getMessage());
            return null;
        }
    }

    private int getAdjustedMaxUses(int originalMaxUses) {
        if (originalMaxUses <= 0) return MAX_USES_CAP;
        return Math.min(originalMaxUses, MAX_USES_CAP);
    }

    private int getAdjustedExperience(int originalExperience, int totalCost) {
        int baseExp = Math.max(1, originalExperience);
        int costBasedExp = Math.max(1, totalCost / 10);
        return baseExp + costBasedExp;
    }

    // --- MÉTODOS AUXILIARES DE DISPLAY NAME (REQUERIDOS) ---

    private void updateVillagerDisplayName(Villager villager) {
        try {
            String professionName = getProfessionName(villager.getProfession());
            String levelName = getLevelName(villager.getVillagerLevel());

            Component displayName = Component.text(professionName + " " + levelName)
                    .color(NamedTextColor.GREEN);
            villager.customName(displayName);
            villager.setCustomNameVisible(true);

        } catch (Exception e) {
            // Ignorar errores en la actualización del nombre
        }
    }

    private String getProfessionName(Villager.Profession profession) {
        if (profession == null) {
            return "Desempleado";
        }

        String professionName = profession.toString().toLowerCase();

        if (professionName.contains("name=")) {
            int start = professionName.indexOf("name=") + 5;
            int end = professionName.indexOf("}", start);
            if (end == -1) end = professionName.length();
            professionName = professionName.substring(start, end);
        }

        return getProfessionDisplayName(professionName);
    }

    private String getProfessionDisplayName(String professionName) {
        switch (professionName) {
            case "none": return "Desempleado";
            case "armorer": return "Herrero";
            case "butcher": return "Carnicero";
            case "cartographer": return "Cartógrafo";
            case "cleric": return "Clérigo";
            case "farmer": return "Granjero";
            case "fisherman": return "Pescador";
            case "fletcher": return "Flechero";
            case "leatherworker": return "Peletero";
            case "librarian": return "Bibliotecario";
            case "mason": return "Albañil";
            case "shepherd": return "Pastor";
            case "toolsmith": return "Herrero de herramientas";
            case "weaponsmith": return "Herrero de armas";
            default:
                return capitalizeFirst(professionName);
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String getLevelName(int level) {
        if (level >= 5) return "Maestro";
        switch (level) {
            case 1: return "Novato";
            case 2: return "Aprendiz";
            case 3: return "Journeyman";
            case 4: return "Experto";
            default: return "Principiante";
        }
    }

    // --- FIN MÉTODOS AUXILIARES DE DISPLAY NAME ---

    private void applyTrade(Player player, MerchantRecipe recipe) {
        ItemStack result = recipe.getResult();

        if (result == null) return;

        if (player.getInventory().addItem(result.clone()).isEmpty()) {
            plugin.getLogger().fine("Item dado a " + player.getName() + ": " + result.getType());
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), result.clone());
            player.sendMessage(Component.text("¡Inventario lleno! El item fue dropeado al suelo.")
                    .color(NamedTextColor.YELLOW));
        }
    }

    private void updatePlayerUI(Player player) {
        try {
            plugin.getScoreboardManager().updatePlayerBoard(player);
            plugin.getScoreboardManager().updatePlayerCashCache(player.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("Error actualizando UI del jugador: " + e.getMessage());
        }
    }
}