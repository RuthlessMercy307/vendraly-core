# Manual Técnico – VendralyCore

## 1. Visión general
VendralyCore es un plugin modular para Purpur/Paper 1.21.8 orientado a servidores survival con fuerte componente RPG. El núcleo combina:

* Autenticación obligatoria con contraseñas cifradas mediante BCrypt y asignación dinámica de roles.
* Economía dual (efectivo + banco) con comandos administrativos y comercio seguro entre jugadores.
* Sistemas RPG de estadísticas, progresión de experiencia, oficios configurables, clanes y combate direccional con barra de stamina.
* Interfaz rica (scoreboard, bossbar, mensajes Adventure) y persistencia YAML por jugador.

La clase `com.vendraly.VendralyCore` extiende `JavaPlugin` y actúa como orquestador creando gestores, registrando comandos/listeners y lanzando tareas periódicas (scoreboard, stamina, cooldowns de habilidades, comprobación de trades).

## 2. Estructura del proyecto
```
src/main/java/com/vendraly/
├── VendralyCore.java             # Punto de entrada del plugin
├── commands/                     # Implementación de cada comando registrado en plugin.yml
├── core/
│   ├── auth/                     # Autenticación, sesiones y cambio de roles
│   ├── clans/                    # Modelo y gestor de clanes, invitaciones y guerras
│   ├── config/                   # Carga y guardado de archivos YAML auxiliares
│   ├── database/                 # PlayerData y UserDataManager (persistencia YAML por jugador)
│   ├── economy/                  # Economía bancaria y efectivo
│   ├── jobs/                     # Oficios configurables y cálculo de recompensas
│   ├── protection/               # Gestión de cofres protegidos y control de acceso
│   ├── roles/                    # Enum de roles predefinidos y aplicador de permisos
│   ├── rpg/
│   │   ├── ability/              # Gestión de habilidades activas/pasivas
│   │   ├── combat/               # Motor de daño y direcciones de ataque
│   │   ├── listener/             # Listeners RPG (loot, XP, habilidades)
│   │   ├── loot/                 # Lectura de tablas de botín configurables
│   │   ├── stamina/              # BossBar y regeneración de stamina
│   │   └── stats/                # Estadísticas RPG y experiencia
│   ├── scoreboard/               # Scoreboard lateral y refresco periódico
│   ├── security/                 # Utilidades criptográficas (BCrypt)
│   ├── trade/                    # Motor de comercio seguro
│   └── tradingui/                # Construcción de la GUI de intercambio
├── listeners/                    # Listeners generales (conexión, economía, combate, etc.)
└── utils/                        # Utilidades comunes (TaskUtil)
```

## 3. Componentes principales
### 3.1 Autenticación y datos
* **AuthManager**: registra/loguea jugadores, mantiene estado de sesión en memoria, resetea roles con `RoleManager` y expone `setPlayerRole` para uso administrativo.
* **AuthListener**: bloquea movimiento, interacción, chat y comandos salvo `/login` y `/register` mientras el jugador no esté autenticado.
* **UserDataManager**: serializa `PlayerData` a `plugins/VendralyCore/userdata/<uuid>.yml`, almacenando contraseña hash, rol, saldos, clan, stats y progreso de oficios.
* **AuthUtil**: envoltorio de BCrypt (`hashPassword`, `checkPassword`).

### 3.2 Economía y comercio
* **CashManager**: gestiona el efectivo robable; métodos `modify`, `give`, `take`, `transferCash` se sincronizan con guardado.
* **EconomyManager**: maneja el saldo bancario seguro y permite transferencias entre jugadores.
* **TradeManager/TradeSession**: registra solicitudes, crea sesiones GUI, restringe slots válidos, controla estado de confirmación y, al finalizar, intercambia ítems y efectivo ofrecido.
* **TradeGuiManager**: construye la interfaz de 54 slots con separadores y botones de confirmación.
* **EconomyListener**: convierte items configurados (ej. lingotes) en efectivo al recogerse y bloquea cofres protegidos.
* **TradeListener**: controla clics dentro del inventario de trade, reinicia confirmaciones cuando cambian los ofrecimientos y cierra la sesión si algún jugador abandona.

### 3.3 Progresión RPG
* **StatManager**: aplica estadísticas personalizadas a atributos de Bukkit, gestiona inversión de puntos (`/stats`) y concede mejoras directas (`rewardAction`).
* **XPManager**: curva de experiencia con subida de nivel automática y asignación de puntos sin gastar.
* **AbilityManager**: registra habilidades desbloqueadas y ejecuta efectos (Berserk, Dodge, etc.) con cooldowns.
* **StaminaManager**: crea BossBars individuales, permite consumir stamina y regenera en un `tick` periódico.
* **CombatManager/DamageEngine/AttackDirection**: calculan dirección basada en yaw, modifican daño y defensa y aplican consumo de stamina.
* **RPGPlayerListener**: otorga experiencia por romper bloques/mobs, ejecuta loot tables y lanza habilidades usando ítems gatillo.
* **LootTableManager**: lee `loot.yml`, genera drops ponderados por rareza y expone `dropLoot` para listeners.

### 3.4 Clanes, oficios y protección
* **ClanManager**: crea clanes persistentes, invita miembros, gestiona guerras (`declareWar`) y mantiene relaciones `jugador → clan`.
* **ClanListener**: impide daño entre miembros y bloquea ataques entre clanes sin guerra declarada.
* **JobManager**: carga oficios desde `jobs.yml`, detecta materiales asociados y recompensa con dinero y XP.
* **JobListener**: invoca `JobManager.reward` al talar/minar/craftear.
* **ProtectionManager/ProtectionListener**: marcan cofres (click agachado con stick) y controlan acceso compartido con miembros del clan.

### 3.5 Interfaz y utilidades
* **ScoreboardManager**: crea scoreboards por jugador mostrando nivel, XP, salud, stamina, fuerza y saldos; se actualiza cada 3 segundos.
* **StaminaManager**: genera barras de stamina en BossBar por jugador.
* **RoleManager**: asigna permisos mediante `PermissionAttachment`, aplica prefijos Adventure al display/tablist.
* **TaskUtil**: atajos para `runTask`, `runTaskLater`, `runTaskTimer` y tareas asíncronas.

## 4. Flujos funcionales
### 4.1 Inicio de sesión
1. `PlayerConnectionListener` carga/crea `PlayerData` en memoria al `PlayerJoinEvent` y llama a `AuthManager.handleJoin`.
2. Si el jugador ya estaba autenticado se reactiva la sesión y se aplican stats/scoreboard; de lo contrario los listeners en `AuthListener` bloquean toda acción hasta completar `/register` o `/login`.
3. Al registrarse o autenticarse correctamente se guardan datos, se aplican roles y se muestran mensajes Adventure.

### 4.2 Economía y comercio
1. `CashManager` y `EconomyManager` centralizan modificaciones de saldo. Los comandos `/eco` y `/pay` usan estos gestores validando permisos y saldos.
2. `/trade <jugador>` envía solicitud (se guarda en `pendingRequests`). `/trade accept` crea un inventario compartido con confirmaciones.
3. Cualquier modificación en la GUI reinicia el estado de “listo”. Cuando ambos confirman se transfieren ítems a la contraparte y se aplican ofertas monetarias utilizando `CashManager.modify`.

### 4.3 Progresión RPG
1. Las acciones relevantes (romper bloques, matar mobs, activar habilidades) generan XP mediante `XPManager` y mejoras puntuales con `StatManager.rewardAction`.
2. `StatManager.apply` recalcula atributos Bukkit (vida, velocidad, daño) usando los valores almacenados en `PlayerData`.
3. `CombatListener` evalúa la dirección de ataque (`AttackDirection.fromYaw`), consume stamina y ajusta el daño final combinando fuerza, agilidad, defensa y resiliencia.
4. `StaminaManager.tick` regenera energía cada medio segundo; al intentar atacar sin stamina el daño se cancela.

### 4.4 Clanes y protecciones
1. `/clan create|invite|join|leave|war` manipula `ClanManager`. Las guerras habilitan daño entre clanes, de lo contrario `ClanListener` cancela el ataque.
2. `ProtectionListener` marca cofres; `ProtectionManager.canAccessChest` permite acceso a propietario o miembros del mismo clan.

## 5. Configuración y persistencia
* **plugin.yml**: declara comandos (`login`, `register`, `eco`, `pay`, `trade`, `clan`, `jobs`, `role`, `stats`, `loot`, `rgpexp`) y permisos (`vendraly.economy.admin`, `vendraly.roles`, `vendraly.rpg.exp`).
* **config.yml**: parámetros generales (`authentication.reminder-interval`, ajustes RPG).
* **jobs.yml**: lista de oficios. Ejemplo:
  ```yaml
  jobs:
    miner:
      name: Minero
      reward: 8
      materials:
        - IRON_ORE
        - GOLD_ORE
  ```
* **loot.yml**: tablas por tipo de mob. Ejemplo:
  ```yaml
  tables:
    zombie:
      drop1:
        material: ROTTEN_FLESH
        min: 1
        max: 3
        rarity: COMMON
  ```
* **roles.yml**: prefijos y permisos por rol. El enum `Role` define los valores por defecto, pero este YAML sirve como referencia para servidores que deseen expandirlo.
* **clans.yml**: clanes persistentes guardados por `ClanManager.save()` con `name`, `leader` y `members`.

## 6. Extensibilidad
* **Nuevos oficios**: añadir entradas a `jobs.yml`. `JobManager` detecta automáticamente los materiales incluidos.
* **Más roles**: ampliar el enum `Role` y reutilizar `AuthManager.setPlayerRole` para persistirlos.
* **Habilidades nuevas**: añadir valores a `AbilityType` y extender `AbilityManager.applyEffect`.
* **Nuevos tipos de loot**: crear nuevas secciones en `loot.yml` usando el nombre de la entidad en minúsculas (`skeleton`, `creeper`, etc.).
* **Comandos adicionales**: implementar `CommandExecutorHolder`, registrar en `VendralyCore.registerCommands()` y declararlos en `plugin.yml`.

## 7. Recomendaciones operativas
* **Seguridad**: nunca almacene contraseñas sin hashing. `AuthManager` usa BCrypt con coste 12; evite reducirlo. Considere configurar `permissions.yml` para reforzar roles.
* **Backups**: los YAML de jugadores y clanes son críticos. Programe copias de seguridad periódicas del directorio `plugins/VendralyCore`.
* **Sincronización de hilos**: cualquier operación que toque Bukkit API debe ejecutarse en el hilo principal. Utilice `TaskUtil.runSync` cuando procese resultados de tareas asíncronas.
* **Pruebas manuales**: tras instalar una versión nueva verifique los flujos principales: `/register` → `/login`, `/eco give`, `/pay`, `/trade`, `/stats add`, `/clan create`, `/clan war`, combate y drops de `loot.yml`.
* **Logs**: la mayoría de errores graves se reportan vía `Logger` del plugin. Active `debug` en servidor para rastrear problemas de permisos o economía.

Este manual debe acompañar cualquier despliegue de VendralyCore y servir como guía para desarrolladores que extiendan el ecosistema o integren nuevos módulos.
