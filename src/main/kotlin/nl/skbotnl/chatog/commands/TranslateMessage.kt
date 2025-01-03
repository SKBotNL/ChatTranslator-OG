package nl.skbotnl.chatog.commands

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import nl.skbotnl.chatog.*
import nl.skbotnl.chatog.Helper.legacyToMm
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class TranslateMessage : CommandExecutor {
    interface ISentMessage {
        val message: String
    }

    data class SentChatMessage(override val message: String, val player: Player) : ISentMessage
    data class SentCustomMessage(
        override val message: String,
        val username: String,
        val prefix: Component,
        val suffix: Component
    ) :
        ISentMessage

    data class SentPMMessage(override val message: String, val sender: UUID, val receiver: UUID) : ISentMessage

    companion object {
        val chatMessages: MutableMap<UUID, SentChatMessage> = HashMap()
        val customMessages: MutableMap<UUID, SentCustomMessage> = HashMap()
        val pmMessages: MutableMap<UUID, SentPMMessage> = HashMap()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) {
            return false
        }
        if (args == null) {
            return false
        }
        if (args.isEmpty()) {
            return false
        }
        if (args.count() < 2) {
            return false
        }
        val player: Player = sender

        if (Helper.getTranslateTimeout(player.uniqueId) != 0L) {
            player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: <red>You're doing that too fast."))
            return true
        }

        val uuid: UUID?

        try {
            uuid = UUID.fromString(args[0])
        } catch (e: IllegalArgumentException) {
            player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: <red>That is not a valid UUID."))
            return true
        }

        val messageType: Int
        try {
            messageType = args[1].toInt()
        } catch (_: NumberFormatException) {
            player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: <red>Invalid message type."))
            return true
        }

        val sentMessage: ISentMessage? = when (messageType) {
            1 -> chatMessages[uuid]
            2 -> customMessages[uuid]
            3 -> pmMessages[uuid]
            else -> {
                return false
            }
        }

        if (sentMessage == null) {
            player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: <red>Could not find that message."))
            return true
        }

        GlobalScope.launch {
            val language = LanguageDatabase.getPlayerLanguage(player.uniqueId)

            if (language == "null") {
                player.sendMessage(
                    ChatOG.mm.deserialize("${Config.prefix}<reset>: <red>Something went wrong will trying to get your language.")
                )
                return@launch
            }

            player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: Translating message (this can take some time)..."))

            val translated = ArgosTranslate.translate(sentMessage.message, language)
            translateCallback(translated, player, messageType, sentMessage, language)
        }

        return true
    }

    private fun translateCallback(
        translated: ArgosTranslate.Translated,
        player: Player,
        messageType: Int,
        sentMessage: ISentMessage?,
        language: String
    ) {
        if (translated.error != null) {
            player.sendMessage(translated.error)
            return
        }

        if (translated.translatedText == null) {
            player.sendMessage(ChatOG.mm.deserialize("${Config.prefix}<reset>: <red>Could not translate that message."))
            return
        }

        lateinit var translateMessage: Component

        when (messageType) {
            1 -> {
                val sentChatMessage = sentMessage as SentChatMessage
                var playerString =
                    "${ChatOG.chat.getPlayerPrefix(sentChatMessage.player)}${sentChatMessage.player.name}${
                        ChatOG.chat.getPlayerSuffix(sentChatMessage.player)
                    }"
                if (PlaceholderAPI.setPlaceholders(sentMessage.player, "%simpleclans_clan_color_tag%") != "") {
                    playerString = PlaceholderAPI.setPlaceholders(
                        sentMessage.player,
                        "&8[%simpleclans_clan_color_tag%&8] $playerString"
                    )
                }
                val playerComponent =
                    ChatOG.mm.deserialize(legacyToMm("<light_purple>[${translated.translatedFrom} -> $language (Can be inaccurate)] $playerString"))

                // Don't convert color in translated messages
                translateMessage = Component.join(
                    JoinConfiguration.noSeparators(),
                    playerComponent,
                    Component.text(translated.translatedText)
                )
            }

            2 -> {
                val sentCustomMessage = sentMessage as SentCustomMessage

                val translatedComponent =
                    Component.text("[${translated.translatedFrom} -> $language (Can be inaccurate)] ")
                        .color(NamedTextColor.LIGHT_PURPLE)

                translateMessage = Component.join(
                    JoinConfiguration.noSeparators(),
                    translatedComponent,
                    sentCustomMessage.prefix,
                    Component.text(sentCustomMessage.username),
                    sentCustomMessage.suffix,
                    Component.text(translated.translatedText)
                )
            }

            3 -> {
                val sentPMMessage = sentMessage as SentPMMessage

                val translatedComponent =
                    Component.text("[${translated.translatedFrom} -> $language (Can be inaccurate)] ")
                        .color(NamedTextColor.LIGHT_PURPLE)

                val pmComponent = if (player.uniqueId == sentPMMessage.sender) {
                    ChatOG.mm.deserialize("<gold>[<red>me <gold>-> <dark_red>${Bukkit.getPlayer(sentPMMessage.receiver)!!.name}<gold>]<white> ")
                } else {
                    ChatOG.mm.deserialize("<gold>[<dark_red>${Bukkit.getPlayer(sentPMMessage.sender)!!.name} <gold>-> <red>me<gold>]<white> ")
                }

                translateMessage = Component.join(
                    JoinConfiguration.noSeparators(),
                    translatedComponent,
                    pmComponent,
                    Component.text(translated.translatedText)
                )
            }
        }
        player.sendMessage(translateMessage)
    }
}
