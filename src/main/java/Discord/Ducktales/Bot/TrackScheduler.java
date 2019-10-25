package Discord.Ducktales.Bot;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import discord4j.core.object.entity.MessageChannel;

public class TrackScheduler extends AudioEventAdapter implements AudioLoadResultHandler {

	private final AudioPlayer player;
	private final BlockingQueue<AudioTrack> queue;
	private MessageChannel outputChannel;
	private Logger logger = new Logger("TrackScheduler-Logger");
	private boolean loop = false;
	private AudioTrack loopTrack;

	/**
	 * @param player The audio player this scheduler uses
	 */
	public TrackScheduler(AudioPlayer player) {
		this.player = player;
		this.queue = new LinkedBlockingQueue<>();
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Print the Current Track-Queue
	 * @param number of next Queue Entrys to show
	 */
	public void showQueue(int count) {
		queue.forEach(track -> logger.debug(track.getInfo().title));
		String output = queue.stream().limit(count).map(track -> track.getInfo().title).collect(Collectors.joining("\n"));
		logger.debug("Show Queue:\n" + output);
		outputChannel.createMessage("Currently running Queue:\n" + output).block();
	}

	/**
	 * Add the next track to queue or play right away if nothing is in the queue.
	 * @param track The track to play or add to queue.
	 */
	public void queue(AudioTrack track) {
		//	Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
		// something is playing, it returns false and does nothing. In that case the player was already playing so this
		// track goes to the queue instead.
		if (!player.startTrack(track, true)) {
			logger.debug("Queueing... new AudioTrack: " + track.getInfo().title);
			queue.offer(track);
			logger.debug("Queued new AudioTrack: " + track.getInfo().title);
			outputChannel.createMessage("Queued new AudioTrack: " + track.getInfo().title).block();
		}
	}

	/**
	 * Clears the entire Queue
	 */
	public void clear() {
		queue.clear();

		String msg = "Cleared Queue";
		this.logger.debug(msg);
		this.outputChannel.createMessage(msg).block();
	}

	/**
	 * Loops the current AudioTrack
	 */
	public void loop() {
		this.loopTrack = player.getPlayingTrack();
		this.loop = true;

		String msg = "Looping Track: " + this.loopTrack.getInfo().title;
		this.logger.debug(msg);
		this.outputChannel.createMessage(msg).block();
	}
	
	/**
	 * Stops the loop for the current AudioTrack
	 */
	public void unloop() {
		this.loop = false;
		String msg = "Stopped looping Track: " + this.loopTrack.getInfo().title;
		this.logger.debug(msg);
		this.outputChannel.createMessage(msg).block();
	}

	/**
	 * Start the next track, stopping the current one if it is playing.
	 */
	public void nextTrack() {
		AudioTrack track;
		if (loop)
			track = this.loopTrack;
		else
			track = queue.poll();

		if (track != null)
			logger.debug("Starting... next AudioTrack: " + track.getInfo().title);
		else
			logger.debug("Cannot start nextTrack. Nothing in the Queue. Stopping the Audioplayer");
		// Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
		// giving null to startTrack, which is a valid argument and will simply stop the player.
		player.startTrack(track, false);
	}

	/**
	 * Stops the Audioplayer. If it is playing a track it skips the track and stops.
	 */
	public void stopPlayer() {
		logger.debug("Stopping...: " + player.getPlayingTrack().getInfo().title);
		player.stopTrack();
	} 

	/**
	 * Pauses the Audioplayer. If it is playing a track it can be resumed with {@link resumePlayer}
	 */
	public void pausePlayer() {
		logger.debug("Pausing...:  Audioplayer");
		player.setPaused(true);
	}
	
	/**
	 * Resumes the Audioplayer. I
	 */
	public void resumePlayer() {
		if (player.isPaused()) {
			logger.debug("Player was paused. Resuming...: Audioplayer");
			player.setPaused(false);
		}
		else {
			logger.debug("Audioplayer was stopped:");
			nextTrack();
		}
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		// endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
		// endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
		// endReason == STOPPED: The player was stopped.
		// endReason == REPLACED: Another track started playing while this had not finished
		// endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a clone of this back to your queue
		
		if (endReason == AudioTrackEndReason.REPLACED) {
			logger.debug("OnTrackEnd: Skipped AudioTrack: " + track.getInfo().title);
		}
		if (endReason == AudioTrackEndReason.STOPPED) {
			logger.debug("OnTrackEnd: The Player was stopped");
		}
		if (endReason == AudioTrackEndReason.CLEANUP) {
			logger.debug("OnTrackEnd: Audioplayer hasn't been queried  for a while");
		}
		// Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED) 
		if (endReason.mayStartNext) {
			nextTrack();
		}
	}

	@Override
	public void onPlayerPause(AudioPlayer player) {
		// Player was paused
		logger.debug("Paused Audioplayer");
		outputChannel.createMessage("Paused Audioplayer").block();
	}

	@Override
	public void onPlayerResume(AudioPlayer player) {
		// Player was resumed
		logger.debug("Resumed Audioplayer");
		outputChannel.createMessage("Resumed Audioplayer").block();
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		// A track started playing
		logger.debug("Started next Track: " + track.getInfo().title);
		this.outputChannel.createMessage("Started next Track: " + track.getInfo().title).block();
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
		// An already playing track threw an exception (track end event will still be received separately)
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		// Audio track has been unable to provide us any audio, might want to just start a new track
	}


	/*
	 * From here it's the AudioLoadResultHandler implementation
	 * Handles the result of loading an item from an audio player manager.
	 */

	@Override
	public void trackLoaded(AudioTrack track) {
		this.queue(track);
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		for (AudioTrack track : playlist.getTracks())
			this.queue(track);
	}

	@Override
	public void noMatches() {
		logger.debug("No Match found");
		this.outputChannel.createMessage("No Match found").block();
	}

	@Override
	public void loadFailed(FriendlyException exception) {
		logger.debug("Load failed: " + exception.getMessage());
		this.outputChannel.createMessage("Load failed: " + exception.getMessage()).block();
	}
}
