package nl.skbotnl.chatog

import club.minnced.discord.webhook.WebhookClient
import nl.skbotnl.chatog.Helper.convertColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ChatConfigReload : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        Config.load()
        BingTranslator.apiKey = Config.getApiKey()
        BingTranslator.subscriptionRegion = Config.getSubRegion()

        DiscordBridge.channelId = Config.getChannelId()
        DiscordBridge.guildId = Config.getGuildId()
        DiscordBridge.webhook = WebhookClient.withUrl(Config.getWebhook())

        sender.sendMessage(convertColor("&aReloaded the config!"))
        return true
    }
}