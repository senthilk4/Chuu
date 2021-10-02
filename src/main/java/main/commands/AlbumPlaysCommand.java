package main.commands;

import dao.DaoImplementation;
import dao.entities.LastFMData;
import main.apis.discogs.DiscogsApi;
import main.apis.discogs.DiscogsSingleton;
import main.apis.spotify.Spotify;
import main.apis.spotify.SpotifySingleton;
import main.exceptions.LastFmEntityNotFoundException;
import main.exceptions.LastFmException;
import main.parsers.ArtistAlbumParser;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.management.InstanceNotFoundException;
import java.util.Collections;
import java.util.List;


public class AlbumPlaysCommand extends ConcurrentCommand {
	private final DiscogsApi discogsApi;
	private final Spotify spotify;

	public AlbumPlaysCommand(DaoImplementation dao) {
		super(dao);
		this.parser = new ArtistAlbumParser(dao, lastFM);
		this.discogsApi = DiscogsSingleton.getInstanceUsingDoubleLocking();
		this.spotify = SpotifySingleton.getInstanceUsingDoubleLocking();
	}

	@Override
	public String getDescription() {
		return ("How many times you have heard an album!");
	}

	@Override
	public List<String> getAliases() {
		return Collections.singletonList("album");
	}

	@Override
	public void onCommand(MessageReceivedEvent e) {
		String[] parsed;
		parsed = parser.parse(e);
		if (parsed == null || parsed.length != 3)
			return;
		String artist = parsed[0];
		String album = parsed[1];
		long whom = Long.parseLong(parsed[2]);
		artist = CommandUtil.onlyCorrection(getDao(), artist, lastFM);
		doSomethingWithAlbumArtist(artist, album, e, whom);

	}

	void doSomethingWithAlbumArtist(String artist, String album, MessageReceivedEvent e, long who) {
		int a;
		try {
			LastFMData data = getDao().findLastFMData(who);

			a = lastFM.getPlaysAlbum_Artist(data.getName(), artist, album).getPlays();
			String usernameString = data.getName();

			usernameString = getUserStringConsideringGuildOrNot(e, who, usernameString);

			String ending = a > 1 ? "times " : "time";

			sendMessageQueue(e, "**" + usernameString + "** has listened **" + album + "** " + a + " " + ending);

		} catch (InstanceNotFoundException ex) {
			parser.sendError(parser.getErrorMessage(5), e);
		} catch (LastFmEntityNotFoundException ex) {
			parser.sendError(parser.getErrorMessage(6), e);
		} catch (LastFmException ex) {
			parser.sendError(parser.getErrorMessage(2), e);
		}
	}

	@Override
	public String getName() {
		return "Get Plays Album";
	}


}