package io.github.moregrayner.chocoKingDrop

import net.md_5.bungee.api.ChatColor
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Slime
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice.ExactChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class ChocoKingDrop : JavaPlugin(), CommandExecutor, Listener {
    private var npcLocation: Location? = null

    override fun onEnable() {
        getCommand("start")?.setExecutor(this)
        Bukkit.getPluginManager().registerEvents(this, this)
    }

    override fun onDisable() {
    }

    private fun startGame() {
        object : BukkitRunnable() {
            override fun run() {
                val server = server
                val players: List<Player> = ArrayList(server.onlinePlayers)

                spread(players)
                Bukkit.broadcastMessage(ChatColor.YELLOW.toString() + "모든 플레이어를 랜덤 좌표로 분산시켰습니다.")
                Bukkit.broadcastMessage(ChatColor.YELLOW.toString() + "테스트 버전이라 전용 리소스 팩이 제공되지 않습니다. 이용에 참고해주세요.")

                val randomPlayer = players[Random().nextInt(players.size)]
                val customCocoa: ItemStack = createCustomItem(
                    Material.COCOA_BEANS, 1,
                    ChatColor.GOLD.toString() + "새까만 코코아 가루", null
                ) // 레시피와 동일한 코코아 가루 지급
                val waypoint: ItemStack = createCustomItem(
                    Material.STICK, 3,
                    ChatColor.AQUA.toString() + "길잡이돌", null
                )
                randomPlayer.inventory.addItem(customCocoa)
                randomPlayer.inventory.addItem(customCocoa)
                randomPlayer.inventory.addItem(customCocoa)

                for (player in players) {
                    player.inventory.addItem(waypoint)
                    player.sendMessage(ChatColor.WHITE.toString() + "길잡이돌이 지급되었습니다!")
                }
                randomPlayer.sendMessage(ChatColor.WHITE.toString() + "[" + ChatColor.GOLD + "코코아" + ChatColor.WHITE + "]" + "가 인벤토리에 지급되었습니다.")

                registerRecipe()
            }
        }.runTaskLater(this, 20L) // 1초 후 시작
    }

    private fun her(world: World) {

        object : BukkitRunnable() {
            override fun run() {
                // 랜덤 위치 생성 (±250 범위 내)
                val x = (Math.random() * 501).toInt() - 250
                val z = (Math.random() * 501).toInt() - 250
                val y = world.getHighestBlockYAt(x, z) + 1

                npcLocation = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

                // 슬라임 소환
                val slime = world.spawnEntity(npcLocation!!, EntityType.SLIME) as Slime
                slime.customName = "그녀"
                slime.size = 20
                slime.isInvulnerable = true
                slime.setAI(false)
                val slimeLocation: Location = npcLocation as Location

                val armorStandLocation: Location =
                    npcLocation!!.clone().add(0.0, slime.height / 2 + 1, 0.0) // 슬라임 크기 높이만큼 위로 이동
                val armorStand = world.spawnEntity(armorStandLocation, EntityType.ARMOR_STAND) as ArmorStand
                armorStand.customName = "그녀"
                armorStand.isCustomNameVisible = true
                armorStand.isInvisible = true
                armorStand.setGravity(false)

                Bukkit.broadcastMessage("그녀 등장!")

                logger.info("슬라임이 생성되었습니다! 위치: " + slimeLocation.x + ", " + slimeLocation.y + ", " + slimeLocation.z)
            }
        }.runTaskLater(this, 6000L)
    }

    private fun registerRecipe() {
        val chocolate = createCustomItem(
            Material.COOKIE, 2,
            ChatColor.GOLD.toString() + "다크초코쿠키",
            mutableListOf(
                ChatColor.WHITE.toString() + "누군가 정성을 다해 만든 초콜릿의 정수가 담긴 쿠키.",
                ChatColor.WHITE.toString() + "하루빨리 그녀에게 전해주어야만 한다."
            )
        )

        val customCocoa: ItemStack = createCustomItem(
            Material.COCOA_BEANS, 1,
            ChatColor.GOLD.toString() + "새까만 코코아 가루", null
        )

        val key = NamespacedKey(this, "Canon")
        val recipe = ShapedRecipe(key, chocolate)
        recipe.shape("SCS", "MCM", "SCS")
        recipe.setIngredient('C', ExactChoice(customCocoa))
        recipe.setIngredient('M', Material.MILK_BUCKET)
        recipe.setIngredient('S', Material.SUGAR)

        Bukkit.addRecipe(recipe)
    }
    private fun createCustomItem(
        material: Material,
        customModelData: Int,
        name: String,
        lore: List<String>?
    ): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        if (meta != null) {
            meta.setCustomModelData(customModelData)
            meta.setDisplayName(name)
            if (lore != null) {
                meta.lore = lore
            }
            item.setItemMeta(meta)
        }
        return item
    }

    //게임로직:진행방식
    private fun spread(players: List<Player>) {
        val random = Random()
        for (player in players) {
            val worldBorder = player.world.worldBorder
            val borderCenter = player.location
            val borderSize = 500.0
            worldBorder.center = borderCenter
            worldBorder.size = borderSize


            val randomLocation = player.world.spawnLocation.clone().add(
                (random.nextInt(501) - 250).toDouble(),
                0.0,
                (random.nextInt(501) - 250).toDouble()
            )
            val highestY = player.world.getHighestBlockYAt(randomLocation.blockX, randomLocation.blockZ)
            randomLocation.y = (highestY + 1).toDouble()

            player.teleport(randomLocation)
        }
    }

    //게임로직:이벤트
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val itemInHand = player.inventory.itemInMainHand

        if (itemInHand.type != Material.AIR && itemInHand.hasItemMeta()) {
            val itemMeta = itemInHand.itemMeta
            val item = event.item

            if (itemMeta != null && itemMeta.hasCustomModelData() && itemMeta.customModelData == 2) {
                for (entity in player.getNearbyEntities(10.0, 10.0, 10.0)) {
                    if (entity is ArmorStand) { //아머스탠드 기반
                        if (entity.getCustomName() != null && entity.getCustomName() == "그녀") {
                            event.isCancelled = true
                            if (itemInHand.type == Material.COOKIE) {
                                player.sendMessage("♡♡♡")
                                for (onlinePlayer in Bukkit.getServer().onlinePlayers) {
                                    if (onlinePlayer != player) {
                                        onlinePlayer.gameMode = GameMode.SPECTATOR
                                    }
                                }
                                Bukkit.broadcastMessage(ChatColor.YELLOW.toString() + player.name + "님이 그녀에게 초콜릿을 전해줬습니다!")
                                clearAllSlimes()
                                val inventory: Inventory = player.inventory
                                for (i in 0 until inventory.size) {
                                    val iteminv = inventory.getItem(i)

                                    if (iteminv != null && iteminv.hasItemMeta()) {
                                        val meta = iteminv.itemMeta
                                        if (meta!!.hasCustomModelData()) {
                                            val customModelData = meta.customModelData

                                            if (customModelData == 1 || customModelData == 2) {
                                                inventory.setItem(i, null)
                                            }
                                        }
                                    }
                                }

                                object : BukkitRunnable() {
                                    override fun run() {
                                        Bukkit.broadcastMessage("예쁜 사랑 하세요!")
                                    }
                                }.runTaskLater(this, 60L)
                            } else {
                                player.sendMessage("♡")
                            }
                            return
                        }
                    }
                }
            }
            if (item != null && item.hasItemMeta()) {
                val meta = item.itemMeta
                if (meta!!.hasCustomModelData() && meta.customModelData == 3) {
                    val target: Player? = findTargetPlayer(player)
                    if (target != null) {
                        createParticleLine(player.location, target.location)
                    } else {
                        player.sendMessage("코코아를 소유한 플레이어를 찾을 수 없습니다.")
                    }
                }
            }
        }
    }

    private fun createParticleLine(start: Location, end: Location) {
        val points = 20
        val step = 1.0 / points

        for (i in 0..points) {
            val t = i * step
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t
            val z = start.z + (end.z - start.z) * t

            val particleLocation = Location(start.world, x, y, z)
            start.world!!.spawnParticle(Particle.CRIT, particleLocation, 1)
        }
    }

    private fun findTargetPlayer(source: Player): Player? {
        val maxDistance = 10.0
        for (target in Bukkit.getOnlinePlayers()) {
            if (target == source) continue

            if (target.world == source.world && target.location.distance(source.location) <= maxDistance) {
                if (hasCustomModelDataItem(target, 1) || hasCustomModelDataItem(target, 2)) {
                    return target
                }
            }
        }
        return null
    }

    private fun hasCustomModelDataItem(player: Player, modelData: Int): Boolean {
        for (item in player.inventory.contents) {
            if (item != null && item.hasItemMeta()) {
                val meta = item.itemMeta
                if (meta!!.hasCustomModelData() && meta.customModelData == modelData) {
                    return true
                }
            }
        }
        return false
    }

    private fun clearAllSlimes() {
        for (world in Bukkit.getWorlds()) {
            for (entity in world.entities) {
                if (entity.type == EntityType.ARMOR_STAND||entity.type == EntityType.SLIME) {
                    entity.remove()
                }
            }
        }
    }
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.")
            return false
        }

        if (command.name == "start") {
            startGame()
            val world = Bukkit.getWorld("world")
            if (world != null) {
                her(world)
            } else {
                sender.sendMessage("월드를 찾을 수 없습니다.")
                return false
            }
            sender.sendMessage("게임을 시작합니다.")
        }

        return true
    }

}
