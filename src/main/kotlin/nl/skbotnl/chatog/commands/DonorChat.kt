package nl.skbotnl.chatog.commands

import nl.skbotnl.chatog.ChatSystemHelper
import nl.skbotnl.chatog.ChatSystemHelper.ChatType
import nl.skbotnl.chatog.Helper.convertColor
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
            sender.sendMessage(convertColor("[&aChat&f-&cOG&f]: &cYou do not have permission to run this command."))
            return true
        }
        if (args == null || args.isEmpty()) {
            if (ChatSystemHelper.inChat[sender.uniqueId] == ChatType.DONORCHAT) {
                ChatSystemHelper.inChat[sender.uniqueId] = ""

                sender.sendMessage(convertColor("[&aChat&f-&cOG&f]: You are now talking in normal chat."))
                return true
            }
            ChatSystemHelper.inChat[sender.uniqueId] = ChatType.DONORCHAT
            sender.sendMessage(convertColor("[&aChat&f-&cOG&f]: You are now talking in donor chat."))
            return true
        }

        ChatSystemHelper.sendMessageInDonorChat(sender, args.joinToString(separator=" "))

        return true
    }
}