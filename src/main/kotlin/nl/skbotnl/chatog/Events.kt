package nl.skbotnl.chatog

import io.papermc.paper.advancement.AdvancementDisplay.Frame.*
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.clip.placeholderapi.PlaceholderAPI
import net.ess3.api.events.VanishStatusChangeEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import nl.skbotnl.chatog.ChatSystemHelper.ChatType
import nl.skbotnl.chatog.Helper.legacyToMm
import nl.skbotnl.chatog.Helper.removeColor
import nl.skbotnl.chatog.commands.TranslateMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.event.server.BroadcastMessageEvent
import xyz.jpenilla.announcerplus.listener.JoinQuitListener
import java.util.*

@OptIn(DelicateCoroutinesApi::class)
class Events : Listener {
    private var lastMessaged: MutableMap<UUID, UUID> = HashMap()

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!Config.discordEnabled) {
            return
        }
        if (ChatOG.essentials.getUser(event.player).isVanished && event.joinMessage() !is TextComponent) {
            return
        }

        var chatString = "${ChatOG.chat.getPlayerPrefix(event.player)}${event.player.name}"

        if (PlaceholderAPI.setPlaceholders(event.player, "%simpleclans_clan_color_tag%") != "") {
            chatString = PlaceholderAPI.setPlaceholders(
                event.player,
                "&8[%simpleclans_clan_color_tag%&8] $chatString"
            )
        }

        GlobalScope.launch {
            DiscordBridge.sendEmbed(
                "${removeColor(chatString)} has joined the game. ${
                    Bukkit.getOnlinePlayers().count()
                } player(s) online.", event.player.uniqueId, 0x00FF00
            )
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if (!Config.discordEnabled) {
            return
        }
        if (ChatOG.essentials.getUser(event.player).isVanished) {
            return
        }

        var chatString = "${ChatOG.chat.getPlayerPrefix(event.player)}${event.player.name}"

        if (PlaceholderAPI.setPlaceholders(event.player, "%simpleclans_clan_color_tag%") != "") {
            chatString = PlaceholderAPI.setPlaceholders(
                event.player,
                "&8[%simpleclans_clan_color_tag%&8] $chatString"
            )
        }

        GlobalScope.launch {
            DiscordBridge.sendEmbed(
                "${removeColor(chatString)} has left the game. ${
                    Bukkit.getOnlinePlayers().count() - 1
                } player(s) online.", event.player.uniqueId, 0xFF0000
            )
        }
    }

    @EventHandler
    fun onKick(event: PlayerKickEvent) {
        if (!Config.discordEnabled) {
            return
        }
        if (ChatOG.essentials.getUser(event.player).isVanished) {
            return
        }

        var chatString = "${ChatOG.chat.getPlayerPrefix(event.player)}${event.player.name}"

        if (PlaceholderAPI.setPlaceholders(event.player, "%simpleclans_clan_color_tag%") != "") {
            chatString = PlaceholderAPI.setPlaceholders(
                event.player,
                "&8[%simpleclans_clan_color_tag%&8] $chatString"
            )
        }

        val reason = PlainTextComponentSerializer.plainText().serialize(event.reason())

        GlobalScope.launch {
            DiscordBridge.sendEmbed(
                "${removeColor(chatString)} was kicked with reason: \"${reason}\". ${
                    Bukkit.getOnlinePlayers().count() - 1
                } player(s) online.", event.player.uniqueId, 0xFF0000
            )
        }
    }

    @EventHandler
    fun onAdvancement(event: PlayerAdvancementDoneEvent) {
        if (!Config.discordEnabled) {
            return
        }
        if (ChatOG.essentials.getUser(event.player).isVanished) {
            return
        }

        var chatString = "${ChatOG.chat.getPlayerPrefix(event.player)}${event.player.name}"

        if (PlaceholderAPI.setPlaceholders(event.player, "%simpleclans_clan_color_tag%") != "") {
            chatString = PlaceholderAPI.setPlaceholders(
                event.player,
                "&8[%simpleclans_clan_color_tag%&8] $chatString"
            )
        }

        val advancementTitleKey = event.advancement.display?.title() ?: return
        val advancementTitle = PlainTextComponentSerializer.plainText().serialize(advancementTitleKey)

        val advancementMessage = when (event.advancement.display?.frame()) {
            GOAL -> "has reached the goal [$advancementTitle]"
            TASK -> "has made the advancement [$advancementTitle]"
            CHALLENGE -> "has completed the challenge [$advancementTitle]"
            else -> {
                return
            }
        }

        GlobalScope.launch {
            DiscordBridge.sendEmbed(
                "${removeColor(chatString)} $advancementMessage.",
                event.player.uniqueId,
                0xFFFF00
            )
        }
    }

    @EventHandler
    fun onBroadcast(event: BroadcastMessageEvent) {
        if (!Config.discordEnabled) {
            return
        }
        if (event.message() !is TextComponent) {
            return
        }

        val content = (event.message() as TextComponent).content()
        if (content == "") {
            return
        }

        GlobalScope.launch {
            DiscordBridge.sendMessage(content, "[Server] Broadcast", null)
        }
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        if (event.isCancelled) return
        event.isCancelled = true

        val oldTextComponent = event.message() as TextComponent

        if (ChatSystemHelper.inChat[event.player.uniqueId] == ChatType.STAFFCHAT) {
            ChatSystemHelper.sendMessageInStaffChat(event.player, oldTextComponent.content())
            return
        }
        if (ChatSystemHelper.inChat[event.player.uniqueId] == ChatType.DONORCHAT) {
            ChatSystemHelper.sendMessageInDonorChat(event.player, oldTextComponent.content())
            return
        }

        var chatString = "${ChatOG.chat.getPlayerPrefix(event.player)}${event.player.name}"

        if (PlaceholderAPI.setPlaceholders(event.player, "%simpleclans_clan_color_tag%") != "") {
            chatString =
                PlaceholderAPI.setPlaceholders(event.player, "&8[%simpleclans_clan_color_tag%&8] $chatString")
        }

        val discordMessageString = Helper.convertEmojis(oldTextComponent.content())

        GlobalScope.launch {
            DiscordBridge.sendMessage(discordMessageString, chatString, event.player.uniqueId)
        }

        val messageComponents = Helper.convertLinks(oldTextComponent.content(), event.player)

        val messageComponent =
            Component.join(JoinConfiguration.separator(Component.text(" ")), messageComponents) as TextComponent

        val chatComponent = ChatOG.mm.deserialize(legacyToMm("$chatString${ChatOG.chat.getPlayerSuffix(event.player)}"))

        var textComponent =
            Component.join(JoinConfiguration.noSeparators(), chatComponent, messageComponent)
        textComponent = textComponent.hoverEvent(
            HoverEvent.hoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                ChatOG.mm.deserialize("<green>Click to translate this message")
            )
        )

        val randomUUID = UUID.randomUUID()
        textComponent = textComponent.clickEvent(
            ClickEvent.clickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/translatemessage $randomUUID 1"
            )
        )

        event.viewers().forEach {
            it.sendMessage(textComponent)
        }

        TranslateMessage.chatMessages[randomUUID] =
            TranslateMessage.SentChatMessage(oldTextComponent.content(), event.player)
    }

    @EventHandler
    fun onCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val checkSplit = event.message.split(" ", limit = 2)[0]

        if (!(checkSplit == "/msg" || checkSplit == "/whisper" || checkSplit == "/pm" || checkSplit == "/reply" || checkSplit == "/r")) {
            if (checkSplit == "/s") {
                event.isCancelled = true

                if (!event.player.hasPermission("chat-og.staff")) {
                    event.player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: <red>You do not have permission to run this command."))
                    return
                }

                val args = event.message.split(" ").drop(1)

                if (args.isEmpty()) {
                    if (ChatSystemHelper.inChat[event.player.uniqueId] == ChatType.STAFFCHAT) {
                        ChatSystemHelper.inChat[event.player.uniqueId] = ""

                        event.player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: You are now talking in normal chat."))
                        return
                    }
                    ChatSystemHelper.inChat[event.player.uniqueId] = ChatType.STAFFCHAT
                    event.player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: You are now talking in staff chat."))
                    return
                }

                ChatSystemHelper.sendMessageInStaffChat(event.player, args.joinToString(separator = " "))
            }
            return
        }
        event.isCancelled = true

        val messageSplit: List<String> = if (checkSplit == "/r" || checkSplit == "/reply") {
            event.message.split(" ", ignoreCase = true, limit = 2)
        } else {
            event.message.split(" ", ignoreCase = true, limit = 3)
        }

        if (messageSplit.count() < 3 && !(checkSplit == "/r" || checkSplit == "/reply")) {
            event.player.sendMessage(ChatOG.mm.deserialize("<red>${messageSplit[0]} <to> <message>"))
            return
        }
        if (messageSplit.count() < 2) {
            event.player.sendMessage(ChatOG.mm.deserialize("<red>${messageSplit[0]} <message>"))
            return
        }

        val player: Player?
        val message: String

        if (checkSplit == "/r" || checkSplit == "/reply") {
            message = messageSplit[1]

            if (!lastMessaged.containsKey(event.player.uniqueId)) {
                event.player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: <red>You haven't messaged anyone yet."))
                return
            }

            val lastMessagedPlayer = lastMessaged[event.player.uniqueId]
            player = Bukkit.getPlayer(lastMessagedPlayer!!)
        } else {
            player = Bukkit.getPlayer(messageSplit[1])
            message = messageSplit[2]
        }

        if (player == null) {
            event.player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: <red>That player doesn't exist or isn't online."))
            return
        }

        val randomUUID = UUID.randomUUID()

        TranslateMessage.pmMessages[randomUUID] = TranslateMessage.SentPMMessage(
            message,
            event.player.uniqueId,
            player.uniqueId
        )

        val colourMessage = if (event.player.hasPermission("chat-og.color")) {
            ChatOG.mm.deserialize(legacyToMm(message))
        } else {
            Component.text(message)
        }

        val toSenderPrefix = "<gold>[<red>me <gold>-> <dark_red>${player.name}<gold>]<white>"
        var toSenderTextComponent = Component.join(
            JoinConfiguration.separator(Component.text(" ")),
            ChatOG.mm.deserialize(toSenderPrefix),
            colourMessage
        )
        toSenderTextComponent = toSenderTextComponent.hoverEvent(
            HoverEvent.hoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                ChatOG.mm.deserialize("<green>Click to translate this message")
            )
        )
        toSenderTextComponent = toSenderTextComponent.clickEvent(
            ClickEvent.clickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/translatemessage $randomUUID 3"
            )
        )
        event.player.sendMessage(toSenderTextComponent)

        val toPrefix = "<gold>[<dark_red>${event.player.name} <gold>-> <red>me<gold>]<white>"
        var toTextComponent = Component.join(
            JoinConfiguration.separator(Component.text(" ")),
            ChatOG.mm.deserialize(toPrefix),
            colourMessage
        )
        toTextComponent = toTextComponent.hoverEvent(
            HoverEvent.hoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                ChatOG.mm.deserialize("<green>Click to translate this message")
            )
        )
        toTextComponent = toTextComponent.clickEvent(
            ClickEvent.clickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/translatemessage $randomUUID 3"
            )
        )
        player.sendMessage(toTextComponent)

        lastMessaged[event.player.uniqueId] = player.uniqueId
        lastMessaged[player.uniqueId] = event.player.uniqueId

        return
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        if (!Config.discordEnabled) {
            return
        }
        if (ChatOG.essentials.getUser(event.player).isVanished) {
            return
        }

        if (event.deathMessage() is TextComponent) {
            GlobalScope.launch {
                DiscordBridge.sendEmbed(
                    removeColor((event.deathMessage() as TextComponent).content()),
                    event.player.uniqueId,
                    0xFF0000
                )
            }
            return
        }

        var nameString = "${ChatOG.chat.getPlayerPrefix(event.player)}${event.player.name}"

        if (PlaceholderAPI.setPlaceholders(event.player, "%simpleclans_clan_color_tag%") != "") {
            nameString =
                PlaceholderAPI.setPlaceholders(event.player, "&8[%simpleclans_clan_color_tag%&8] $nameString")
        }
        val nameComponent = ChatOG.mm.deserialize(legacyToMm(nameString))

        var oldDeathMessage = event.deathMessage() as TranslatableComponent
        oldDeathMessage = oldDeathMessage.color(TextColor.color(16755200))
        oldDeathMessage = oldDeathMessage.append(Component.text("."))

        val argList = oldDeathMessage.args().toMutableList()
        argList[0] = nameComponent
        val deathMessage = oldDeathMessage.args(argList)

        event.deathMessage(deathMessage)

        val translatedDeathMessage = PlainTextComponentSerializer.plainText().serialize(deathMessage)

        GlobalScope.launch {
            DiscordBridge.sendEmbed(removeColor(translatedDeathMessage), event.player.uniqueId, 0xFF0000)
        }
    }

    @EventHandler
    fun onVanish(event: VanishStatusChangeEvent) {
        if (event.value) {
            val playerQuitEvent = PlayerQuitEvent(
                event.affected.base,
                Component.translatable(
                    "multiplayer.player.left",
                    NamedTextColor.YELLOW,
                    event.affected.base.displayName()
                ),
                PlayerQuitEvent.QuitReason.DISCONNECTED
            )
            JoinQuitListener().onQuit(playerQuitEvent)
            onQuit(playerQuitEvent)
        } else {
            val playerJoinEvent = PlayerJoinEvent(
                event.affected.base,
                Component.text("")
            )
            JoinQuitListener().onJoin(playerJoinEvent)
            onJoin(playerJoinEvent)
        }
    }
}
