package jdmusicbot;

import java.util.List;
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
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import net.dv8tion.jda.api.entities.MessageChannel;

//import discord4j.core.object.entity.MessageChannel;

public class TrackScheduler extends AudioEventAdapter implements AudioLoadResultHandler {

	private final AudioPlayer player;
	private final BlockingQueue<AudioTrack> queue;
	private MessageChannel outputChannel;
	private Logger logger = new Logger("TrackScheduler-Logger");
	private boolean loop = false;
	private AudioTrack loopTrack;
    private AudioTrack lastTrack;

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

	private long getSeconds(long milliseconds) {
		return (milliseconds / 1000) % 60;
	}

	private long getMinutes(long milliseconds) {
		return (milliseconds / 1000) / 60;
	}

	/**
	 * Print the Current Track-Queue
	 * @param number of next Queue Entrys to show
	 */
	public void showQueue(int count) {
		AudioTrack ctrack = player.getPlayingTrack();
		String output = "Currently running Queue:\n";
		/* get currently Playing track if one is playing */
		if (ctrack != null) {
			long pos = ctrack.getPosition();
			long milli = ctrack.getInfo().length;
			output += String.format("%s [%d:%02d / %d:%02d]\n", ctrack.getInfo().title, this.getMinutes(pos), this.getSeconds(milli), this.getMinutes(milli), this.getSeconds(pos));
		}

		output += queue.stream()
			.limit(count)
			.map(track -> String.format("%s [%d:%02d]", track.getInfo().title, getMinutes(track.getInfo().length), getSeconds(track.getInfo().length)))
			.collect(Collectors.joining("\n"))
			.toString();

		logger.debug(output);
		/* Split String into Chunks of 1500 size (because of Discords max message length)*/
		String[] outputs = output.split("(?s)(?<=\\G.{1000})");
		for (String out : outputs) {
			outputChannel.sendMessage(App.MSG_PREFIX + out + App.MSG_POSTFIX).queue();
		}
	}

	/**
	 * Add the next track to queue or play right away if nothing is in the queue.
	 * @param track The track to play or add to queue.
	 */
	public void queue(AudioTrack track) {
		//	Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
		// something is playing, it returns false and does nothing. In that case the player was already playing so this
		// track goes to the queue instead. Passing null will just stop the current track and return false
		if (!player.startTrack(track, true)) {
			logger.debug("Queueing... new AudioTrack: " + track.getInfo().title);
			queue.offer(track);
			String formatted = String.format("Queued new AudioTrack: %s [%d:%02d]", track.getInfo().title, getMinutes(track.getInfo().length), getSeconds(track.getInfo().length));
			logger.debug(formatted);
			outputChannel.sendMessage(App.MSG_PREFIX + formatted + App.MSG_POSTFIX).queue();
		}
	}
    
    public void repeat() {
        if (this.lastTrack != null) {
            this.queue(this.lastTrack);
        }
    }

	public void queuePlaylist(AudioPlaylist playlist) {
		List<AudioTrack> tracks = playlist.getTracks();
		// Get playing length of entire Playlist in milliseconds
		long playlistLength = tracks.stream()
			.mapToLong(track -> track.getInfo().length)
			.reduce(0, Long::sum);
		logger.debug("Queueing... new Playlist: " + playlist.getName());
		//	Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
		// something is playing, it returns false and does nothing. In that case the player was already playing so this
		// track goes to the queue instead.
		// If the first Track of the playlist could successfully play, remove it from the tracks to be queued
		if (player.startTrack(tracks.get(0), true))
			tracks.remove(0);

		tracks.forEach(track -> queue.offer(track));
		String formatted = String.format("Queued new Playlist: %s [%d:%02d]", playlist.getName(), getMinutes(playlistLength), getSeconds(playlistLength));
		logger.debug(formatted);
		outputChannel.sendMessage(App.MSG_PREFIX + formatted + App.MSG_POSTFIX).queue();
	}

	/**
	 * Clears the entire Queue
	 */
	public void clear() {
		queue.clear();

		String msg = "Cleared Queue";
		this.logger.debug(msg);
		this.outputChannel.sendMessage(App.MSG_PREFIX + msg + App.MSG_POSTFIX).queue();
	}

	/**
	 * Loops the current AudioTrack
	 */
	public void loop() {
		/* Very Important to use 'makeClone()' because each AudioTrack saves it's current execution state and beacause of that we can't use it twice with the same reference */
		this.loopTrack = player.getPlayingTrack().makeClone();
		this.loop = true;

		String msg = "Looping Track: " + this.loopTrack.getInfo().title;
		this.logger.debug(msg);
		this.outputChannel.sendMessage(App.MSG_PREFIX + msg + App.MSG_POSTFIX).queue();
	}
	
	/**
	 * Stops the loop for the current AudioTrack
	 */
	public void unloop() {
		this.loop = false;

		String msg = "Stopped looping Track: " + this.loopTrack.getInfo().title;
		this.logger.debug(msg);
		this.outputChannel.sendMessage(App.MSG_PREFIX + msg + App.MSG_POSTFIX).queue();
	}

	/**
	 * Start the next track, stopping the current one if it is playing.
	 */
	public void nextTrack() {
		AudioTrack track;
		if (loop) {
		/* Very Important to use 'makeClone()' because each AudioTrack saves it's current execution state and beacause of that we can't use it twice with the same reference */
			track = this.loopTrack.makeClone();
		}
		else {
			track = queue.poll();
		}

		if (track != null) {
			logger.debug("Starting... next AudioTrack: " + track.getInfo().title);
		}
		else {
			logger.debug("Cannot start next Track. Nothing left in the Queue. Stopping the Audioplayer");
		}
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
            lastTrack = track.makeClone(); // set the last played track
			nextTrack();
		}
	}

	@Override
	public void onPlayerPause(AudioPlayer player) {
		// Player was paused
		logger.debug("Paused Audioplayer");
		outputChannel.sendMessage(App.MSG_PREFIX + "Paused Audioplayer" + App.MSG_POSTFIX).queue();
	}

	@Override
	public void onPlayerResume(AudioPlayer player) {
		// Player was resumed
		logger.debug("Resumed Audioplayer");
		outputChannel.sendMessage(App.MSG_PREFIX + "Resumed Audioplayer" + App.MSG_POSTFIX).queue();
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		// A track started playing
		String formatted = String.format("Started next Track: %s [%d:%02d]",track.getInfo().title,  getMinutes(track.getInfo().length), getSeconds(track.getInfo().length));
		logger.debug(formatted);

		this.outputChannel.sendMessage(App.MSG_PREFIX + formatted + App.MSG_POSTFIX).queue();
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
		// An already playing track threw an exception (track end event will still be received separately)
		logger.debug("Playing Track: " + track.getInfo().title + " threw an exception: " + exception.getMessage());
        nextTrack();
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		// Audio track has been unable to provide us any audio, might want to just start a new track
		String msg = "Track got stuck: " + track.getInfo().title + " ThresholdMS: " + thresholdMs + " Starting next Track";
		logger.debug(msg);
		outputChannel.sendMessage(App.MSG_PREFIX + msg + App.MSG_POSTFIX).queue();
		nextTrack();
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
		if (playlist.isSearchResult()) {
			this.queue(playlist.getTracks().get(0));
		} else {
			this.queuePlaylist(playlist);
		}
	}

	@Override
	public void noMatches() {
		logger.debug("No Match found");
		this.outputChannel.sendMessage(App.MSG_PREFIX + "No Match found" + App.MSG_POSTFIX).queue();
	}

	@Override
	public void loadFailed(FriendlyException exception) {
		logger.debug("Load failed: " + exception.getMessage());
		this.outputChannel.sendMessage(App.MSG_PREFIX + "Load failed: " + exception.getMessage() + App.MSG_POSTFIX).queue();
	}
}
