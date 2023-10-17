package nl.skbotnl.chatog.commands

import nl.skbotnl.chatog.ChatOG
import nl.skbotnl.chatog.ChatSystemHelper
import nl.skbotnl.chatog.ChatSystemHelper.ChatType
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DonorChat : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) {
            sender.sendMessage("You can only execute this command as a player")
            return true
        }
        if (!sender.hasPermission("chat-og.donors")) {
            sender.sendMessage(ChatOG.mm.deserialize("[<green>Chat<white>-<red>OG<white>]: <red>You do not have permission to run this command."))
            return true
        }
        if (args == null || args.isEmpty()) {
            if (ChatSystemHelper.inChat[sender.uniqueId] == ChatType.DONORCHAT) {
                ChatSystemHelper.inChat[sender.uniqueId] = ""

                sender.sendMessage(ChatOG.mm.deserialize("[<green>Chat<white>-<red>OG<white>]: You are now talking in normal chat."))
                return true
            }
            ChatSystemHelper.inChat[sender.uniqueId] = ChatType.DONORCHAT
            sender.sendMessage(ChatOG.mm.deserialize("[<green>Chat<white>-<red>OG<white>]: You are now talking in donor chat."))
            return true
        }

        ChatSystemHelper.sendMessageInDonorChat(sender, args.joinToString(separator = " "))

        return true
    }
}