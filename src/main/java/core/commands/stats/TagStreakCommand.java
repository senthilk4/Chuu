package core.commands.stats;

import core.Chuu;
import core.apis.discogs.DiscogsApi;
import core.apis.discogs.DiscogsSingleton;
import core.apis.spotify.Spotify;
import core.apis.spotify.SpotifySingleton;
import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.exceptions.LastFmException;
import core.imagerenderer.util.bubble.StringFrequency;
import core.parsers.OnlyUsernameParser;
import core.parsers.OptionalEntity;
import core.parsers.Parser;
import core.parsers.params.ChuuDataParams;
import core.services.AlbumValidator;
import core.services.ColorService;
import core.services.TrackValidator;
import core.services.tracklist.TagStorer;
import core.util.UniqueBag;
import dao.ChuuService;
import dao.entities.*;
import dao.utils.LinkUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Credits: to flushed_emoji bot owner for the idea. Aka stolen completely
 */
public class TagStreakCommand extends ConcurrentCommand<ChuuDataParams> {
    private final DiscogsApi discogsApi;
    private final Spotify spotifyApi;

    public TagStreakCommand(ChuuService dao) {
        super(dao);
        discogsApi = DiscogsSingleton.getInstanceUsingDoubleLocking();
        spotifyApi = SpotifySingleton.getInstance();

    }


    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.USER_STATS;
    }

    @Override
    public Parser<ChuuDataParams> initParser() {
        return new OnlyUsernameParser(db, new OptionalEntity("start", "show the moment the streak started"));
    }

    @Override
    public String getDescription() {
        return "Genre combo";
    }

    @Override
    public List<String> getAliases() {
        return List.of("tagstreak", "tagcombo", "genrecombo", "genretag", "gcombo", "gtag");
    }

    @Override
    public String getName() {
        return "Genre streak";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull ChuuDataParams params) throws LastFmException {


        LastFMData user = params.getLastFMData();
        String lastfmId = user.getName();
        long discordID = user.getDiscordId();
        Map<String, Long> artistToId = new HashMap<>();
        Map<AlbumInfo, Long> albumToId = new HashMap<>();
        Map<TrackInfo, Long> trackToId = new HashMap<>();
        UniqueBag<String> tagCombo = lastFM.getTagCombo(user, (artist, track, album) -> {
            Set<String> accumTags = null;
            try {
                Long artistId = artistToId.get(artist);
                if (artistId == null) {
                    ScrobbledArtist sa = new ScrobbledArtist(artist, 0, null);
                    CommandUtil.validate(db, sa, lastFM, null, null, false, true);
                    artistToId.put(artist, sa.getArtistId());
                    artistId = sa.getArtistId();
                }
                accumTags = db.getArtistTag(artistId);

                TrackInfo ti = new TrackInfo(artist, album, track, null);
                Long trackId = trackToId.get(ti);
                if (trackId == null) {
                    ScrobbledTrack sTr = new TrackValidator(db, lastFM).validate(artistId, artist, track);
                    trackToId.put(ti, sTr.getTrackId());
                    trackId = sTr.getTrackId();
                }
                accumTags.addAll(db.getTrackTags(trackId));

                if (album != null) {
                    AlbumInfo ai = new AlbumInfo(album, artist);
                    Long albumId = albumToId.get(ai);
                    if (albumId == null) {
                        ScrobbledAlbum sAlb = new AlbumValidator(db, lastFM).validate(artistId, artist, album);
                        albumToId.put(ai, sAlb.getAlbumId());
                        albumId = sAlb.getAlbumId();
                    }
                    accumTags.addAll(db.getAlbumTags(albumId));
                }
                if (accumTags.isEmpty()) {
                    accumTags = new HashSet<>(new TagStorer(db, lastFM, executor, new NowPlayingArtist(artist, null, true, album, track, null, lastfmId)).findTags(15));
                }
                return accumTags;
            } catch (Exception ex) {
                Chuu.getLogger().warn(ex.getMessage(), ex);
                if (accumTags == null || accumTags.isEmpty())
                    return null;
                return accumTags;
            }
        });
        if (tagCombo.isEmpty()) {
            sendMessageQueue(e, "Couldn't find any tag combo on your history :(");
            return;
        }

        DiscordUserDisplay userInformation = CommandUtil.getUserInfoConsideringGuildOrNot(e, discordID);
        String userName = userInformation.getUsername();
        String userUrl = userInformation.getUrlImage();

        StringBuilder description = new StringBuilder();


        tagCombo.stream().map(t -> new StringFrequency(t, tagCombo.getSize(t))).filter(t -> t.freq() > 1)
                .sorted(Comparator.comparingInt(StringFrequency::freq).reversed()).limit(5).forEach(z -> description.append("**[%s](%s)**: ".formatted(z.key(), LinkUtils.getLastFmTagUrl(z.key()))).
                append(z.freq()).append(z.freq() >= 2000 ? "+" : "").append(" consecutive plays\n"));


        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor(String.format("%s's current tag streak", CommandUtil.markdownLessUserString(userName, discordID, e)), CommandUtil.getLastFmUser(lastfmId), userUrl)
                .setColor(ColorService.computeColor(e))
                .setDescription(description);
        e.getChannel().sendMessage(embedBuilder.build()).
                queue();
    }
}