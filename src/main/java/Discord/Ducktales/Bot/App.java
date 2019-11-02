/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package Discord.Ducktales.Bot;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

//import org.discordbots.api.client.DiscordBotListAPI;
//import org.discordbots.api.client.DiscordBotListAPI.Builder;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;

public class App {

	private static Logger logger = new Logger("Command-Logger");
	private static final DiscordClient client = new DiscordClientBuilder("Mjg3MzI4MzU5Njc4MjE0MTU1.XayxVg.OOMDFgF66DVClhe_HTBfThB5_cA").build();

	public static final String CMD_PREFIX = "#";
	public static final String MSG_PREFIX = "```";
	public static final String MSG_POSTFIX = "```";

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> client.logout().block(Duration.ofSeconds(1))));
		Logger.addLogFile(new File("/var/log/DucktalesBot.log"));
		/*
		   DiscordBotListAPI api = new Builder()
		   .token("OeUB7zCt8ITuPF-5yfa1ALUhbrMNR-qk")
		   .botId("287328359678214155")
		   .build();
		   */
		client.getEventDispatcher().on(ReadyEvent.class)
			.subscribe(ready -> logger.debug("Logged in as " + ready.getSelf().getUsername()));
		
		client.getEventDispatcher().on(MessageCreateEvent.class)
			.subscribe(event -> handleMsg(event));

		/* Lavaplayer */
		playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);

		AudioPlayer player = playerManager.createPlayer();

		/* Register our TrackScheduler that is receiving all audio events from lavaplayer (like events for starting or ending tracks). It makes sense to also make it responsible for scheduling tracks */
		trackScheduler = new TrackScheduler(player);
		player.addListener(trackScheduler);

		/* Link Discord4j and Lavaplayer together */ 
		provider = new LavaPlayerAudioProvider(player);

		/* Initialize Command Queue */
		initializeCommands();


		client.login().block();

		logger.debug("Logged out as " + client.getSelf().block().getUsername());
	}

	private static AudioProvider provider;

	private static AudioPlayerManager playerManager;
	private static TrackScheduler trackScheduler;

	private static void handleMsg(MessageCreateEvent event) {
		/* Set Bot Output Channel for more Output */
		trackScheduler.setOutputChannel(event.getMessage().getChannel().block());

		final String content = event.getMessage().getContent().orElse("");
		for (final Map.Entry<String, CommandInfo> entry : commands.entrySet()) {
			if (content.startsWith(CMD_PREFIX + entry.getKey())) {
				entry.getValue().cmd.execute(event);
				break;
			}
		}

	}

	private static Map<String, CommandInfo> commands = new HashMap<String, CommandInfo>();

	/* Holds the currently active Connections to a Voice-Chat and is being used to disconnect from a Voice-Channel */
	private static Map<Long, VoiceConnection> vcons = new HashMap<Long, VoiceConnection>();

	private static void initializeCommands() {
		/* help Command */
		commands.put("help", new CommandInfo("help", "Show the Help Message", event -> {
			MessageChannel channel = event.getMessage().getChannel().block();
			StringBuilder builder = new StringBuilder();
			for (final Map.Entry<String, CommandInfo> entry : commands.entrySet()) {
				CommandInfo info = entry.getValue();
				builder.append("Command/Usage: ");
				builder.append(CMD_PREFIX + info.usage);
				builder.append("\nDescription: ");
				builder.append(info.description);
				builder.append("\n\n");
			}
			channel.createMessage(MSG_PREFIX + builder.toString() + MSG_POSTFIX).block();
		}));

		/* ping Command */
		commands.put("ping", new CommandInfo("ping", "get yourself a pong", event -> {
			event.getMessage().getChannel().block().createMessage(MSG_PREFIX + "Pong" + MSG_POSTFIX).block();
		}));

		/* join Command */
		commands.put("join", new CommandInfo("join", "let Ducktales Bot join your voice channel", event -> {
			final Member member = event.getMember().orElse(null);
			if (member != null) {
				final VoiceState voiceState = member.getVoiceState().block();
				if (voiceState != null) {
					final VoiceChannel channel = voiceState.getChannel().block();
					if (channel != null) {
						logger.debug("At join Command: Try to Join VoiceChannel");
						App.vcons.put(channel.getGuildId().asLong(), channel.join(spec -> spec.setProvider(provider)).block(Duration.ofSeconds(2)));
						logger.debug("At join Command: Tried to Join VoiceChannel. Either Successed or is timed out.");
					}
				}
			}
		}));

		/* play Command */
		commands.put("play", new CommandInfo("play [youtube-video-link or Soundcloud or ...]", "Let the Ducktales Bot join your voice channel. If no Track is currently playing it's directly started and if not it's appended to the Audio-Queue", event -> {
			final Member member = event.getMember().orElse(null);
			if (member != null) {
				final VoiceState voiceState = member.getVoiceState().block();
				if (voiceState != null) {
					final VoiceChannel channel = voiceState.getChannel().block();
					if (channel != null) {
						logger.debug("At play Command: Get VoiceChannel of Ducktales Bot");
						//VoiceChannel schan = client.getSelf().block().asMember(channel.getGuildId()).block().getVoiceState().block().getChannel().block();
						VoiceChannel schan = client.getSelf().flatMap(user -> user.asMember(channel.getGuildId())).flatMap(mem -> mem.getVoiceState()).flatMap(vstate -> vstate.getChannel()).block();
						logger.debug("At play Command: Check if Ducktales Bot is already in the VoiceChannel");
						if (!channel.equals(schan)) {
							/* If you are not in the voice channel of the caller, join the voice channel */
							logger.debug("At play Command: Joining... VoiceChannel");
							App.vcons.put(channel.getGuildId().asLong(), channel.join(spec -> spec.setProvider(provider)).block());
						}
						else {
							logger.debug("At play Command: Ducktales Bot already in the VoiceChannel");
						}
						logger.debug("At play Command: Load Audio-Track");
						String url = event.getMessage().getContent().get().split(" ")[1];
						playerManager.loadItem(url, trackScheduler);
					}
					else {
						logger.debug("At play Command: User is not in a Voicechannel");
					}
				}
				else {
					logger.debug("At play Command: Can't get Voicestate of User");
				}
			}
			else {
				logger.debug("At play Command: Can't get Member of User");
			}
		}));

		/* search Command */
		commands.put("search", new CommandInfo("search [youtube-search-query]", "Search for an Youtube Video and play the first result", event -> {
			final Member member = event.getMember().orElse(null);
			if (member != null) {
				final VoiceState voiceState = member.getVoiceState().block();
				if (voiceState != null) {
					final VoiceChannel channel = voiceState.getChannel().block();
					if (channel != null) {
						logger.debug("At search Command: Get VoiceChannel of Ducktales Bot");
						//VoiceChannel schan = client.getSelf().block().asMember(channel.getGuildId()).block().getVoiceState().block().getChannel().block();
						VoiceChannel schan = client.getSelf().flatMap(user -> user.asMember(channel.getGuildId())).flatMap(mem -> mem.getVoiceState()).flatMap(vstate -> vstate.getChannel()).block();
						logger.debug("At search Command: Check if Ducktales Bot is already in the VoiceChannel");
						if (!channel.equals(schan)) {
							/* If you are not in the voice channel of the caller, join the voice channel */
							logger.debug("At play Command: Joining... VoiceChannel");
							App.vcons.put(channel.getGuildId().asLong(), channel.join(spec -> spec.setProvider(provider)).block());
						}
						else {
							logger.debug("At search Command: Ducktales Bot already in the VoiceChannel");
						}
						logger.debug("At search Command: Load Audio-Track");
						String url = event.getMessage().getContent().get();
						url = url.substring(url.indexOf(' '));
						playerManager.loadItem("ytsearch: " + url, trackScheduler);
					}
					else {
						logger.debug("At search Command: User is not in a Voicechannel");
					}
				}
				else {
					logger.debug("At search Command: Can't get Voicestate of User");
				}
			}
			else {
				logger.debug("At search Command: Can't get Member of User");
			}
		}));

		commands.put("list", new CommandInfo("list [count]", "Lists the next 10 Tracks or the next [count] tracks if specified", event -> {
			String[] args = event.getMessage().getContent().get().split(" ");
			if (args.length > 1)
				trackScheduler.showQueue(Integer.parseInt(args[1]));
			else
				trackScheduler.showQueue(10);
		}));

		commands.put("stop", new CommandInfo("stop", "Skips the currently playing AudioTrack and stops the Audioplayer", event -> trackScheduler.stopPlayer()));

		commands.put("pause", new CommandInfo("pause", "Pauses the Audioplayer", event -> trackScheduler.pausePlayer()));

		commands.put("resume", new CommandInfo("resume", "Resumes the Audioplayer if it's paused or if it is stopped", event -> trackScheduler.resumePlayer()));

		commands.put("skip", new CommandInfo("skip", "Skips the currently playing audio", event -> trackScheduler.nextTrack()));

		commands.put("clear", new CommandInfo("clear", "Clears the entire Queue", event -> trackScheduler.clear()));

		commands.put("loop", new CommandInfo("loop", "Loops the current Track", event -> trackScheduler.loop()));

		commands.put("unloop", new CommandInfo("unloop", "Stops the loop for the current AudioTrack", event -> trackScheduler.unloop()));

		commands.put("test", new CommandInfo("test", "Test", event -> {
			MessageChannel channel = event.getMessage().getChannel().block();
			client.getGuilds().collectList().block().forEach(guild -> channel.createMessage(MSG_PREFIX + "Group-ID: " + guild.getId().asString() + "\nGroup-Name: " + guild.getName() + MSG_POSTFIX).block());
			channel.createMessage(MSG_PREFIX + "Self-ID: " + client.getSelfId().get().asString() + "\nSelf-Name: " + client.getSelf().block().getUsername() + MSG_POSTFIX).block();
			playerManager.loadItem("https://www.youtube.com/watch?v=fzQ6gRAEoy0", trackScheduler);
		}));

		commands.put("leave", new CommandInfo("leave", "Tell the Ducktales Bot to leave it's current voice-channel", event -> {
			long discordServer = event.getGuildId().get().asLong();
			String serverName = client.getGuildById(Snowflake.of(discordServer)).block().getName();
			MessageChannel channel = event.getMessage().getChannel().block();
			if (vcons.containsKey(discordServer)) {
				VoiceConnection vcon = vcons.remove(discordServer);
				logger.debug("At leave Command: Disconnect from VoiceChannel of Discord-Server: " + serverName);
				vcon.disconnect();
				channel.createMessage(MSG_PREFIX + "Disconnected from VoiceChannel of Discord-Server: " + serverName + MSG_POSTFIX).block();
			}
			else {
				logger.debug("At leave Command: Bot is not in a VoiceChannel on this Server");
				channel.createMessage(MSG_PREFIX + "I am not in a VoiceChannel on Discord-Server: " + serverName + MSG_POSTFIX).block();
			}
		}));
		commands.put("logout", new CommandInfo("logout", "Exit the Ducktales Bot Program", event -> {
			logger.debug("Logging out...: " + client.getSelf().block().getUsername());
			client.logout().block();
		}));
	}
}
