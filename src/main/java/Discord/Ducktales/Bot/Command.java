package Discord.Ducktales.Bot;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public interface Command {
	public void execute(MessageCreateEvent event);
	/* Reactive Way */
	// public Mono<Void> execute(MessageCreateEvent event);
}
