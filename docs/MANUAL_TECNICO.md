# Manual Técnico – VendralyCore

## 1. Overview del plugin
VendralyCore es un plugin modular para servidores Paper/Purpur 1.21.8 que unifica autenticación, economía dual (banco y efectivo), comercio seguro, progresión RPG avanzada y personalizaciones de interfaz (scoreboard, nametags). La clase `Main` actúa como orquestador: crea los gestores principales, registra comandos/eventos y pone en marcha tareas periódicas como regeneración de estadísticas, spawners de zonas y actualización de scoreboards. El diseño se apoya en un modelo de datos persistente (`PlayerData` + `UserDataManager`) y sobre él se construyen capas funcionales que operan en memoria (`AuthManager`, `EconomyManager`, `StatManager`, etc.).

Características clave:
- **Autenticación & Roles**: registro/login con BCrypt, asignación dinámica de roles y permisos, gestión de sesiones y caché en memoria.
- **Economía dual**: separación entre saldo bancario y efectivo robable con operaciones asíncronas para evitar bloqueos en el hilo principal.
- **Sistema RPG**: estadísticas personalizadas, habilidades artesanales, experiencia y niveles, combate direccional, gestión de stamina, ítems con metadatos/lore y control de zonas con dificultad.
- **Comercio**: interfaz GUI con confirmación bilateral, bloqueo de slots y ofertas de dinero integradas con la economía.
- **UI dinámica**: scoreboard lateral con información RPG/económica y name-tags coloreados por rol.
- **Integración general**: escuchas para chat, eventos de jugador, comercio con aldeanos, restricciones de loot y requerimientos de equipo.

## 2. Árbol de carpetas y responsabilidades
```
vendraly-core/
├── pom.xml                         # Configuración Maven, dependencias Paper, BCrypt y SnakeYAML
├── src/main/java/
│   └── com/vendraly/
│       ├── commands/               # Implementaciones de comandos administrativos y de usuario
│       ├── core/                   # Núcleo del plugin (gestores, listeners y sistemas RPG)
│       │   ├── auth/               # Autenticación, roles y caché de PlayerData
│       │   ├── database/           # Persistencia YAML de datos de jugador
│       │   ├── economy/            # Lógica de saldo bancario y efectivo
│       │   ├── listeners/          # Listeners generales del núcleo
│       │   ├── rpg/                # Sistema RPG (estadísticas, combate, ítems, mundo)
│       │   │   ├── ability/        # Gestión de profesiones/skills activas
│       │   │   ├── combat/         # Motor de daño y combate direccional
│       │   │   ├── item/           # Metadatos de ítems y actualización de lore
│       │   │   ├── listener/       # Listeners específicos del RPG
│       │   │   └── stats/          # Aplicadores de atributos, menús y regeneración
│       │   ├── roles/              # Enumeración de roles con permisos y apariencia
│       │   ├── scoreboard/         # Scoreboard lateral y name-tags
│       │   ├── security/           # Utilidades criptográficas
│       │   └── trade/              # Sistema de trade seguro entre jugadores
│       ├── listeners/              # Listeners fuera del núcleo (ej. PlayerJoin)
│       └── utils/                  # Utilidades específicas (NMS)
├── src/main/resources/
│   ├── config.yml                  # Parámetros de XP, economía y opciones generales
│   ├── plugin.yml                  # Declaración Bukkit (nombre, comandos, permisos)
│   └── userData/                   # Carpeta de almacenamiento YAML por jugador
└── docs/
    └── MANUAL_TECNICO.md           # Este manual
```

## 3. Clases y responsabilidades
Las siguientes tablas detallan cada clase, su rol y las funciones principales con las que interactúa.

### 3.1 Núcleo (`com.vendraly.core`)
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **Main** | Punto de entrada `JavaPlugin`. Inicializa gestores (fase 1–4), registra comandos/listeners y arranca/paraliza tareas periódicas. | `onEnable()`, `onDisable()`, `registerCommands()`, `registerListeners()`, `setupConfiguration()`, getters de gestores. |
| **WorldDifficultyManager** | Gestiona configuración de dificultad por mundo y su persistencia. | `loadConfiguration()`, `saveConfiguration()`, `getWorldDifficulty()`. |
| **ZoneSpawner** | Controla `SpawnZone` configurables y la tarea programada de respawn de mobs personalizados. | `startSpawnerTask()`, `stopSpawnerTask()`, `spawnEntities()`. |
| **StatManager** | Fachada del sistema RPG: aplica atributos, muestra menú `/stats`, administra regeneración/stamina y delega a submódulos. | `onPlayerJoin()`, `updatePlayerVisuals()`, `openStatMenu()`, `startRegenScheduler()`, `recalculateEquippedBonuses()`. |
| **XPManager** | Curva de experiencia, sincronización con barra vanilla y asignación de puntos de atributo. | `addXP()`, `getXPForNextLevel()`, `updateVanillaXPBar()`, `getUnspentStatPoints()`. |
| **PlayerLevelUpEvent** | Evento Bukkit custom para notificar subidas de nivel. | Constructor con nivel anterior/nuevo. |
| **ParryManager** | Listener para mecánicas de parry/perfect block, maneja cooldowns y feedback. | `onPlayerInteract()`, `onEntityDamage()`. |
| **MonsterListener** | Listeners de spawn/daño para mobs RPG (drop de loot y escalado por zona). | Eventos `onEntityDeath`, `onEntityDamage`. |
| **RPGItemGenerator** | Crea ítems con atributos/lore a partir de plantillas y `QualityUtility`. | `generateItem()`, `applyAttributes()`. |
| **QualityUtility** | Calcula calidades (común, raro, etc.) y sus modificadores numéricos. | `rollQuality()`, `getAttributeBonus()`. |
| **AttributeFormatter** | Formatea atributos numéricos a texto coloreado para tooltips/lore. | `formatAttributeLine()`, `formatPercentage()`. |
| **RPGMonster** | Modelo de mobs custom con stats base, loot y comportamiento. | Getters/constructores, `applyDifficultyScaling()`. |
| **AttributeType** | Enum que clasifica atributos de ítems (DAÑO, DEFENSA, etc.) con llaves internas. | Métodos `getDisplayName()`, `getLoreFormat()`. |
| **RPGZoneCommand** (en `commands`) interactúa con `ZoneSpawner`, ver sección comandos. |

### 3.2 Autenticación, datos y seguridad
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **AuthManager** (`core.auth`) | Caché de `PlayerData`, registro/login con BCrypt, control de roles y sesiones. | `registerPlayer()`, `loginPlayer()`, `logoutPlayer()`, `setPlayerRole()`, `applyRolePermissions()`. |
| **UserDataManager** (`core.database`) | Persistencia YAML: carga/guarda datos, inicializa nuevos jugadores, maneja historial de roles. | `loadPlayerData()`, `savePlayerData()`, `loadPlayerDataAsync()`, `saveRPGStats()`, `initializeNewPlayer()`. |
| **PlayerData** (`core.database`) | POJO con auth, economía, rol y `RPGStats`. | Getters/setters; se inicializa con defaults seguros. |
| **AuthUtil** (`core.security`) | Utilidades de contraseña (hash, verificación, rehash). | `hashPassword()`, `checkPassword()`, `needsRehash()`. |

### 3.3 Economía (`core.economy`)
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **EconomyManager** | Gestiona saldo bancario seguro (no robable), con operaciones asíncronas usando `Executor`. | `getBalance()`, `modifyBalance()`, `transferBalance()`. |
| **CashManager** | Maneja efectivo robable sincronizado con el inventario (cash_balance). | `getBalance()`, `modifyBalance()`, `transferCash()`, métodos síncronos `give()/take()` para comandos. |

### 3.4 Comercio (`core.trade`)
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **TradeManager** | Coordina solicitudes de trade, expiración y sesiones activas. | `requestTrade()`, `acceptTrade()`, `cancelTrade()`, `startExpirationTimer()`, `endTrade()`. |
| **TradeSession** | Estado de un intercambio entre dos jugadores: inventario, ofertas de dinero, estado de aceptación. | `toggleReady()`, `setMoneyOffer()`, `resetReadyStatus()`, `completeTrade()`, `returnItems()`. |
| **TradeGUI** | Construye inventario de trade, define slots, ítems de control y texto informativo. | `createTradeInventory()`, constantes de slots (`P1_INCREASE_10`, etc.). |

### 3.5 Scoreboard y presentación (`core.scoreboard`)
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **ScoreboardManager** | Sidebar personalizada: cachea efectivo, muestra rol, nivel, fama, etc. Gestiona tarea periódica y actualización de NameTags. | `startUpdateTask()`, `updatePlayerBoard()`, `updatePlayerCashCache()`, `clearAllCache()`. |
| **NameTagManager** | Gestiona equipos del scoreboard principal para prefijos/tablist según rol. | `updatePlayerTag()`, `removePlayerTag()`, `updateAllTags()`. |

### 3.6 Listeners generales (`core.listeners` y `com.vendraly.listeners`)
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **PlayerListener** | Bloquea acciones pre-login, orquesta `onLoginSuccess`, libera movimiento/scoreboard y sincroniza stats. | Eventos `onPlayerJoin()`, `onPlayerMove()`, `onCommandPreprocess()`, `onBlockBreak/Place()`, `onPlayerQuit()`, método `onLoginSuccess()`. |
| **TradeListener** | Valida clics en GUI de trade, botones de dinero y cierre cancelado. | `onInventoryClick()`, `onInventoryDrag()`, `onInventoryClose()`. |
| **VillagerTradeListener** | Reemplaza recetas con cobro en cash, prohíbe Mending y valida autenticación. | `onInventoryOpen()`, `convertRecipeToCash()`, `onTradeSelect()`. |
| **ChatListener** | Prefijos de rol en chat y sanitización de mensajes según permisos. | `onAsyncPlayerChat()`. |
| **LootRestrictionListener** | Bloquea apertura/loot de contenedores especiales según reglas. | Eventos de inventario/saqueo. |
| **StatListener** | Propaga cambios de equipo y daño al `StatManager` (actualiza atributos y HUD). | `onInventoryClick()`, `onEntityDamage()`. |
| **ItemRequirementListener** | Comprueba requisitos de nivel/habilidad antes de equipar ítems. | `onPlayerItemHeld()`, `onInventoryClick()`. |
| **CameraChangeListener** | Detecta cambios de perspectiva para reiniciar combos de ataque direccional. | `onPlayerToggleSneak()`, `onPlayerMove()`. |
| **DirectionalAttackListener** | Interpreta inputs del jugador y delega ataques al `DirectionalAttackManager`. | `onPlayerInteract()`, `onPlayerAnimation()`. |
| **PlayerJoinListener** (`com.vendraly.listeners`) | Listener ligero que inicializa scoreboard/stats para conexiones (usado antes del rediseño). | `onPlayerJoin()`. |

### 3.7 Sistema RPG – Submódulos

#### 3.7.1 Estadísticas (`core.rpg.stats`)
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **AttributeApplier** | Traduce `RPGStats` a atributos vanilla (salud, velocidad, daño) y recalcula bonos de equipo. | `applyPlayerAttributes()`, `recalculateStats()`, `recalculateEquippedBonuses()`, `getRpgMaxHealth()`. |
| **StaminaBossBarManager** | Gestiona barras BossBar de estamina: crear, actualizar, remover. | `addStaminaBossBar()`, `updateStaminaBossBar()`, `removeStaminaBossBar()`. |
| **RegenerationScheduler** | Tarea repetitiva que regenera vida/estamina y controla cooldowns de stamina. | `startRegenScheduler()`, `stopRegenScheduler()`, `ensurePlayerTask()`. |
| **StaminaRegenTask** | Implementación puntual de `BukkitRunnable` para regeneración (usado en modo alternativo). | `run()`, control de tasa y cancelación. |
| **LevelingManager** | Gestiona gasto de puntos de atributo y curva interna de atributos (`StatType`). | `investPoint()`, `refundPoint()`, `getPointCost()`. |
| **MenuBuilder** | Genera inventario GUI `/stats` mostrando atributos actuales, costos y botones de asignación. | `openStatMenu()`, `buildStatInventory()`, `handleClick()`. |
| **StatListeners** | Set de listeners auxiliares para sincronizar stats cuando cambian atributos/vida. | Eventos `onHealthChange()`, `onRegenTick()`. |
| **StatType** | Enum de atributos (SALUD, FUERZA, DEFENSA, etc.) con iconos y costos base. | Métodos `getDisplayName()`, `getDescription()`. |

#### 3.7.2 Abilidades (`core.rpg.ability`)
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **AbilityManager** | Registro de habilidades profesionales, XP y efectos pasivos. | `addAbilityExp()`, `getAbilityLevel()`, `applyCraftingBonuses()`. |
| **AbilityType** | Enum con tipos de habilidad (BLACKSMITHING, TAILORING, APOTHECARY, etc.) y metadatos. | `getDisplayName()`, `getLoreColor()`. |
| **BlacksmithingListener** | Bonus a herramientas/armas crafteadas y riesgo de durabilidad según nivel. | `onCraftItem()`, `applyBlacksmithingBonus()`. |
| **TailoringListener** | Bonus de armaduras y control de lore usando `ItemMetadataKeys`. | `onCraftItem()`, `applyTailoringBonus()`. |
| **ApothecaryListener** | Mejora de pociones y riesgos (explosión) dependiendo del nivel. | `onBrew()`, `applyPotionBonus()`. |

#### 3.7.3 Ítems (`core.rpg.item`)
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **ItemMetadataKeys** | Define claves de `PersistentDataContainer` (nivel requerido, calidad, etc.). | `registerKeys()`, getters de `NamespacedKey`. |
| **ItemLoreUpdater** | Reconstruye lore de ítems según calidad, atributos y requisitos; se invoca al equipar/craftear. | `updateItemLore()`, `applyQualityLore()`, `refreshInventory()`. |
| **CraftingManager** | Motor de crafteo custom: valida materiales y asigna atributos/quality. | `handleCraft()`, `buildResultItem()`. |
| **CraftPreviewListener** | Sincroniza vista previa del resultado de crafteo con los cálculos del `CraftingManager`. | `onPrepareItemCraft()`. |
| **PDCUtils** | Utilidades para leer/escribir metadatos en `PersistentDataContainer`. | `setString()`, `getInt()`, `hasKey()`. |

#### 3.7.4 Listeners RPG (`core.rpg.listener`)
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **EquipmentListener** | Observa cambios de armadura/arma y refresca atributos en `StatManager`. | `onInventoryClick()`, `onPlayerSwapHandItems()`. |
| **CraftingListener** | Captura eventos de crafteo para aplicar lógicas personalizadas y otorgar XP de habilidad. | `onCraftItem()`, `onPrepareItemCraft()`. |
| **ItemLoreUpdaterListener** | Enlaza `ItemLoreUpdater` con eventos de inventario/entidades. | `onInventoryClick()`, `onEntityPickupItem()`. |

#### 3.7.5 Combate (`core.rpg.combat`)
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **DamageEngine** | Calcula daño final usando stats personalizados; expone `init()` estático y métodos `computeDamage()`. |
| **DirectionalAttackManager** | Coordina ataques direccionales, combos y tiempos; se alimenta de input del jugador. | `handleAttackInput()`, `startCombo()`, `stop()`. |
| **CombatListener** | Listener de combate que aplica `DamageEngine` y condiciones especiales. | `onEntityDamage()`, `onPlayerAttack()`. |
| **AttackDirection** | Enum de direcciones (arriba, abajo, izquierda, derecha, thrust) con vectores y offsets. |

#### 3.7.6 Mundo y mobs
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **SpawnZone** | DTO para zonas de spawn configurables (ubicación, radio, tipo de mob, límite). | Getters, `fromConfig()`. |
| **ZoneSpawner** | *(ver sección 3.1)* |
| **WorldDifficultyManager** | *(ver sección 3.1)* |

### 3.8 Comandos (`com.vendraly.commands`)
| Comando | Función | Detalles clave |
|---------|---------|----------------|
| **LoginCommand** | Valida contraseña con `AuthManager` y desbloquea al jugador vía `PlayerListener.onLoginSuccess()`. | Requiere argumento `<contraseña>` y bloquea reintentos fallidos. |
| **RegisterCommand** | Registra nuevo jugador (mismo flujo de login), aplica validaciones básicas de longitud/confirmación. | Usa `AuthManager.registerPlayer()` y mensajes informativos. |
| **TestCommand** | Comando diagnóstico `/testcore` que confirma el estado del plugin. | Envía mensaje coloreado al jugador/console. |
| **EconomyCommand** | Administra saldo (`/eco give/take/set/balance`) en cash o banco, usando promesas asíncronas. | Valida permisos `vendraly.economy.admin`, refresca scoreboard. |
| **PayCommand** | Transferencia voluntaria de cash entre jugadores con comprobaciones de saldo. | Delegado a `CashManager.transferCash()`. |
| **TradeCommand** | Comando multifunción para solicitar/aceptar/cancelar trade. | Interactúa con `TradeManager` y muestra mensajes de estado al solicitante/receptor. |
| **SetRoleCommand** | Cambia el rol de un jugador, actualizando permisos y NameTag. | Usa `AuthManager.setPlayerRole()`, valida permiso administrativo. |
| **VendralyBanCommand / VendralyUnbanCommand** | Gestionan estado de ban manual (escritura en YAML y expulsión). | Integran con `UserDataManager`. |
| **RpgExpCommand** | Permite otorgar XP RPG administrativamente. | Llama a `XPManager.addXP()`. |
| **RPGZoneCommand** | Administra zonas de spawn (`add`, `remove`, `list`), persiste en config. | Interfaz a `ZoneSpawner`. |

### 3.9 Roles y utilidades
| Clase | Responsabilidades | Métodos/Focos principales |
|-------|------------------|---------------------------|
| **Role** (`core.roles`) | Enum con permisos, prefijo Adventure y flag `isOp()` por rango (OWNER, MODERADOR, VIP...). | `getFormattedPrefix()`, `getPermissions()`, `getColor()`. |
| **PlayerJoinListener** (`com.vendraly.listeners`) | Mensajería de bienvenida y hooks básicos de login (antes de `PlayerListener`). | `onPlayerJoin()`. |
| **NMSHealthUtil** (`com.vendraly.utils`) | Oculta barra de vida vanilla usando NMS/pakets y fuerza sincronización de salud. | `hideVanillaHealth()`, `sendUpdateAttributesPacket()`. |

## 4. Funciones principales y relaciones
- **Ciclo de autenticación**: `PlayerListener.onPlayerJoin()` bloquea acciones y consulta `AuthManager`. `LoginCommand`/`RegisterCommand` llaman a `AuthManager` que actualiza `PlayerData` vía `UserDataManager`; al éxito se invoca `PlayerListener.onLoginSuccess()` que aplica stats (`StatManager`/`AttributeApplier`), oculta vida (`NMSHealthUtil`), sincroniza XP (`XPManager`) y actualiza scoreboard (`ScoreboardManager`).
- **Persistencia**: todos los gestores que modifican datos (`AuthManager`, `EconomyManager`, `CashManager`, `XPManager`, `StatManager`) delegan en `UserDataManager` para guardar. Operaciones que podrían bloquear se envían a `Executor` asíncrono.
- **Economía**: `EconomyCommand`, `PayCommand` y `TradeSession` consumen `EconomyManager`/`CashManager`. El scoreboard refresca los saldos cacheados con `CashManager.getBalance()` en tareas periódicas.
- **Comercio seguro**: `TradeCommand` abre sesión en `TradeManager`, que crea un `TradeSession` y construye la GUI con `TradeGUI`. `TradeListener` vigila la interacción para evitar fraude, y al completar se transfieren ítems/dinero usando `CashManager`.
- **Sistema RPG**: `StatManager` centraliza. `XPManager` otorga puntos y dispara `PlayerLevelUpEvent`. `LevelingManager` y `MenuBuilder` gestionan la inversión de puntos via `/stats`. `AttributeApplier` recalcula atributos al equipar (`EquipmentListener`) o ganar nivel. `DamageEngine` usa los stats para daño, `DirectionalAttackManager` controla la lógica de combos y `CombatListener` aplica los resultados. `ZoneSpawner` y `MonsterListener` coordinan apariciones y drops en función de `WorldDifficultyManager`.
- **Contenido de ítems**: `CraftingManager`, `ItemLoreUpdater` y listeners asociados aseguran que cualquier ítem generado refleje calidad (`QualityUtility`) y requisitos (`ItemMetadataKeys`). `ItemRequirementListener` impide equipamiento si el jugador no cumple.
- **Integración con aldeanos**: `VillagerTradeListener` reemplaza ingredientes por “cash” y cobra a través de `CashManager`, manteniendo coherencia con el sistema económico.
- **UI**: `ScoreboardManager` y `NameTagManager` se actualizan en tareas programadas; consumen datos de `AuthManager`, `StatManager` y `CashManager`. `PlayerListener.onLoginSuccess()` asegura que la UI sólo se active para jugadores autenticados.

## 5. Notas técnicas y dependencias
- **Dependencias Maven**: `io.papermc.paper:paper-api` (provided), `org.yaml:snakeyaml` para persistencia YAML, `org.mindrot:jbcrypt` para hashing de contraseñas. (`pom.xml`).
- **Versión de Java**: compilado con Java 17 (`maven-compiler-plugin`).
- **Servidor objetivo**: Paper/Purpur 1.21.8; las APIs Adventure se usan para texto, y ciertas utilidades (`NMSHealthUtil`) dependen de NMS específico de la versión.
- **Persistencia**: Archivos YAML por jugador bajo `plugins/VendralyCore/userData`. `UserDataManager` cachea `YamlConfiguration` para minimizar disco.
- **Asincronía**: Operaciones de economía y carga de datos se ejecutan en `Executor` propio para evitar bloqueo. Accesos a Bukkit deben volver al hilo principal (`Bukkit.getScheduler().runTask`).
- **Configuración**: `config.yml` define economía inicial, parámetros de XP y ajustes de mundo. `plugin.yml` declara comandos y permisos; cualquier cambio debe reflejarse ahí para que Bukkit los registre.
- **Extensibilidad**: nuevos roles pueden añadirse extendiendo el enum `Role` y actualizando `AuthManager`/`NameTagManager`. Para nuevas habilidades, agregar al enum `AbilityType` y listeners correspondientes.

## 6. Buenas prácticas y notas operativas
- **Secuencia de inicio**: no instanciar gestores que dependan de otros antes de tiempo (ej. `DamageEngine.init(statManager)` se realiza tras crear `StatManager`).
- **Guardado al apagar**: `Main.onDisable()` detiene tareas (scoreboard, spawners, regen), cancela stamina y fuerza guardado de datos/dificultad.
- **Bloqueo de no autenticados**: cualquier listener/comando adicional debe consultar `PlayerListener.isUnauthenticated()` para mantener coherencia.
- **Sincronización de inventarios**: al modificar ítems vía `ItemLoreUpdater`, ejecutar en el hilo principal y considerar refrescar menús abiertos.
- **Errores comunes**: operaciones asíncronas no pueden acceder directamente a Bukkit API; usar `runTask`. Verificar que cualquier modificación de `YamlConfiguration` finalice con `savePlayerConfig()`.
- **Testing manual**: utilizar `/testcore` tras deploy, crear usuarios de prueba (`/register`, `/login`), validar scoreboard, iniciar trade (`/trade <jugador>`), otorgar XP (`/rgpexp <jugador> <cantidad>`) y revisar logs para advertencias del `TradeManager` o `ScoreboardManager`.

Este documento sirve como referencia interna para desarrolladores que necesiten extender o mantener VendralyCore, ofreciendo un mapa actualizado de responsabilidades y relaciones entre componentes.
