package com.general_hello.commands.commands;

import com.general_hello.commands.database.DataUtils;
import com.jagrosh.jdautilities.command.UserContextMenu;
import com.jagrosh.jdautilities.command.UserContextMenuEvent;
import net.dv8tion.jda.api.entities.Member;

public class AddRemoveBanMenu extends UserContextMenu {
    public AddRemoveBanMenu() {
        this.name = "Ban/Unban (Owner)";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(UserContextMenuEvent event) {
        Member targetMember = event.getTargetMember();
        long targetUser = targetMember.getIdLong();
        if (!DataUtils.hasAccount(targetUser)) {
            event.reply("The user hasn't made an account.").setEphemeral(true).queue();
            return;
        }
        boolean isBanned = DataUtils.isBanned(targetUser);
        DataUtils.setBan(targetUser, !isBanned);
        event.reply((isBanned ? "Unbanned" : "Banned") + " " +
                targetMember.getAsMention() + " from the bot successfully!")
                .setEphemeral(true)
                .queue();
    }
}