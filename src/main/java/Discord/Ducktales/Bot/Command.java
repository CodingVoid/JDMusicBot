package Discord.Ducktales.Bot;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

interface Command {
	public void execute(MessageCreateEvent event);
}
