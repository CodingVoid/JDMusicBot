package jdmusicbot;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public interface Command {
	public void execute(GuildMessageReceivedEvent event);
}
