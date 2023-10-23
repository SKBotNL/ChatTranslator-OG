package nl.skbotnl.chatog

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

object Helper {
    private var translateTimeout: MutableMap<UUID, Long> = HashMap()

    fun getTranslateTimeout(uuid: UUID): Long {
        val cooldown: Long
        if (!translateTimeout.containsKey(uuid)) {
            translateTimeout[uuid] = System.currentTimeMillis()
        }
        if (System.currentTimeMillis() - translateTimeout[uuid]!! > 1000) {
            translateTimeout[uuid] = System.currentTimeMillis()
            cooldown = 0
        } else {
            cooldown = System.currentTimeMillis() - translateTimeout[uuid]!!
        }
        return cooldown
    }

    fun legacyToMm(text: String): String {
        return text.replace("&4", "<dark_red>").replace("&c", "<red>").replace("&6", "<gold>").replace("&e", "<yellow>")
            .replace("&2", "<dark_green>").replace("&a", "<green>").replace("&b", "<aqua>").replace("&3", "<dark_aqua>")
            .replace("&1", "<dark_blue>").replace("&9", "<blue>").replace("&d", "<light_purple>")
            .replace("&5", "<dark_purple>").replace("&f", "<white>").replace("&7", "<gray>")
            .replace("&8", "<dark_gray>").replace("&0", "<black>")
    }

    private val colorRegex = Regex("[§&]?[§&]([0-9a-fk-orA-FK-OR])")
    fun removeColor(text: String): String {
        var tempText = text
        colorRegex.findAll(text).iterator().forEach {
            tempText = tempText.replace(it.value, "")
        }
        return tempText
    }

    fun getColorSection(text: String): String {
        val it = colorRegex.findAll(text).iterator()

        var last = ""
        while (it.hasNext()) {
            last = it.next().value
        }

        return last
    }

    fun getFirstColorSection(text: String): String {
        val it = colorRegex.findAll(text).iterator()

        var first = ""
        while (it.hasNext()) {
            first = it.next().value
            break
        }

        return first
    }

    private val getColorRegex = Regex("(&)?&([0-9a-fk-orA-FK-OR])")
    private fun getColor(text: String): String {
        val it = getColorRegex.findAll(text).iterator()

        var last = ""
        while (it.hasNext()) {
            last = it.next().value
        }

        return last
    }

    // 2 systems because Discord is still rolling out the new username system
    // TODO: Remove legacy username system
    private val getLegacyUsername = Regex("@(.*)#(\\d{4})")
    private val getHandle = Regex("@([a-z0-9_.]{2,32})")
    suspend fun convertMentions(text: String): String {
        val guild = DiscordBridge.jda?.getGuildById(DiscordBridge.guildId)
        if (guild == null) {
            ChatOG.plugin.logger.warning("Can't get the guild, is guildId set?")
            return text
        }

        var tempText = text
        val members = guild.loadMembers().await()

        getLegacyUsername.findAll(text).iterator().forEach { legacy ->
            for (member in members) {
                if (member.user.name.lowercase() == legacy.groupValues[1].lowercase() && member.user.discriminator == legacy.groupValues[2]) {
                    tempText = tempText.replace(legacy.value, member.asMention)
                    return@forEach
                }
            }
        }

        getHandle.findAll(tempText).iterator().forEach { handle ->
            for (member in members) {
                if (member.user.discriminator == "0000" && member.user.name.lowercase() == handle.groupValues[1].lowercase()) {
                    tempText = tempText.replace(handle.value, member.asMention)
                }
            }
        }

        return tempText
    }

    private val urlRegex =
        Regex("(.*)((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;()]*[-a-zA-Z0-9+&@#/%=~_|()])(.*)")

    fun convertLinks(text: String, player: Player): MutableList<Component> {
        val messageComponents = mutableListOf<Component>()

        text.split(" ").forEach { word ->
            val urlIter = urlRegex.findAll(word).iterator()
            val chatColor = getColor(ChatOG.chat.getPlayerSuffix(player))

            if (urlIter.hasNext()) {
                urlIter.forEach { link ->
                    if (BlocklistManager.checkUrl(word)) {
                        player.sendMessage(ChatOG.mm.deserialize("<red>WARNING: You are not allowed to post links like that here."))
                        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                            if (onlinePlayer.hasPermission("group.moderator")) {
                                onlinePlayer.sendMessage(
                                    ChatOG.mm.deserialize(
                                        "[<green>Chat<white>-<red>OG<white>]: ${player.name} has posted a disallowed link: ${
                                            word.replace(
                                                ".",
                                                "[dot]"
                                            )
                                        }."
                                    )
                                )
                            }
                        }
                        return messageComponents
                    }

                    var linkComponent = Component.text(link.groups[2]!!.value).color(TextColor.color(34, 100, 255))
                    linkComponent = linkComponent.hoverEvent(
                        HoverEvent.hoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            ChatOG.mm.deserialize("<green>Click to open link")
                        )
                    )

                    linkComponent = linkComponent.clickEvent(
                        ClickEvent.clickEvent(
                            ClickEvent.Action.OPEN_URL,
                            link.groups[2]!!.value
                        )
                    )

                    val beforeComponent = ChatOG.mm.deserialize(legacyToMm(chatColor) + (link.groups[1]?.value ?: ""))
                    val afterComponent = ChatOG.mm.deserialize(legacyToMm(chatColor) + (link.groups[4]?.value ?: ""))

                    val fullComponent =
                        Component.join(JoinConfiguration.noSeparators(), beforeComponent, linkComponent, afterComponent)

                    messageComponents += fullComponent
                }
                return@forEach
            }
            val wordComponent = if (player.hasPermission("chat-og.color")) {
                if (messageComponents.isNotEmpty()) {
                    val lastContent = (messageComponents.last() as TextComponent).content()
                    if (getColorSection(lastContent) != "" && getFirstColorSection(word) == "") {
                        ChatOG.mm.deserialize(legacyToMm(getColorSection(lastContent) + word))
                    } else {
                        ChatOG.mm.deserialize(legacyToMm(chatColor + word))
                    }
                } else {
                    ChatOG.mm.deserialize(legacyToMm(chatColor + word))
                }
            } else {
                Component.join(
                    JoinConfiguration.noSeparators(),
                    ChatOG.mm.deserialize(legacyToMm(chatColor)),
                    Component.text(word)
                )
            }
            messageComponents += wordComponent
        }

        return messageComponents
    }

    private val emojiRegex = Regex(":(.*?):")

    fun convertEmojis(text: String): String {
        var discordMessageString = text
        var guildEmojis: List<RichCustomEmoji>? = null

        try {
            guildEmojis = DiscordBridge.jda?.getGuildById(DiscordBridge.guildId)?.emojis
        } catch (e: Exception) {
            ChatOG.plugin.logger.warning("Can't get the guild's emojis, is the guildId set?")
        }

        if (guildEmojis != null) {
            emojiRegex.findAll(text).iterator().asSequence().distinctBy { it.value }.forEach {
                guildEmojis.forEach { emoji ->
                    if (emoji.name == it.groupValues[1]) {
                        val replaceWith = "<${if (emoji.isAnimated) "a" else ""}:${it.groupValues[1]}:${emoji.id}>"
                        discordMessageString = discordMessageString.replace(it.value, replaceWith)
                    }
                }
            }
        }

        return discordMessageString
    }

    fun stripGroupMentions(text: String): String {
        var tempText = text.replace("@everyone", "@\u200Eeveryone", false)
        tempText = tempText.replace("@here", "@\u200Ehere", false)

        tempText = tempText.replace("(<@&)(\\d*>)".toRegex(), "$1\u200E$2")

        return tempText
    }
}
