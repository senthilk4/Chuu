package core.commands.whoknows;

import core.apis.discogs.DiscogsApi;
import core.apis.discogs.DiscogsSingleton;
import core.apis.spotify.Spotify;
import core.apis.spotify.SpotifySingleton;
import core.commands.Context;
import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.ChuuEmbedBuilder;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.commands.utils.PrivacyUtils;
import core.exceptions.LastFmException;
import core.imagerenderer.GraphicUtils;
import core.imagerenderer.WhoKnowsMaker;
import core.imagerenderer.util.pie.IPieableList;
import core.imagerenderer.util.pie.PieableListKnows;
import core.otherlisteners.Reactionary;
import core.parsers.DaoParser;
import core.parsers.params.CommandParameters;
import core.parsers.utils.Optionals;
import dao.ServiceView;
import dao.entities.ReturnNowPlaying;
import dao.entities.WKMode;
import dao.entities.WhoKnowsMode;
import dao.entities.WrapperReturnNowPlaying;
import dao.utils.LinkUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import org.imgscalr.Scalr;
import org.knowm.xchart.PieChart;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.EnumSet;

public abstract class WhoKnowsBaseCommand<T extends CommandParameters> extends ConcurrentCommand<T> {
    final DiscogsApi discogsApi;
    final Spotify spotify;
    private final IPieableList<ReturnNowPlaying, T> pie;

    public WhoKnowsBaseCommand(ServiceView dao, boolean isLongRunningCommand) {
        super(dao, isLongRunningCommand);
        if (this.parser instanceof DaoParser<?> p) {
            p.setExpensiveSearch(true);
            p.setAllowUnaothorizedUsers(true);
        }
        pie = new PieableListKnows<>(parser);
        parser.addOptional(Optionals.LIST.opt);
        this.respondInPrivate = false;
        this.discogsApi = DiscogsSingleton.getInstanceUsingDoubleLocking();
        this.spotify = SpotifySingleton.getInstance();
        order = 2;
    }

    public WhoKnowsBaseCommand(ServiceView dao) {
        this(dao, false);

    }

    public static WhoKnowsMode getEffectiveMode(WhoKnowsMode whoKnowsMode, CommandParameters chartParameters) {
        boolean pie = chartParameters.hasOptional("pie");
        boolean list = chartParameters.hasOptional("list");
        if ((whoKnowsMode.equals(WhoKnowsMode.LIST) && !list && !pie) || (!whoKnowsMode.equals(WhoKnowsMode.LIST) && list)) {
            return WhoKnowsMode.LIST;
        } else if (whoKnowsMode.equals(WhoKnowsMode.PIE) && !pie || !whoKnowsMode.equals(WhoKnowsMode.PIE) && pie) {
            return WhoKnowsMode.PIE;
        } else {
            return WhoKnowsMode.IMAGE;
        }
    }

    @Override
    public CommandCategory initCategory() {
        return CommandCategory.WHO_KNOWS;
    }

    @Override
    protected void onCommand(Context e, @Nonnull T params) throws LastFmException {


        WhoKnowsMode whoknowsMode = getWhoknowsMode(params);
        WrapperReturnNowPlaying wrapperReturnNowPlaying = generateWrapper(params, whoknowsMode);
        if (wrapperReturnNowPlaying == null) {
            return;
        }
        generateWhoKnows(wrapperReturnNowPlaying, params, e.getAuthor().getIdLong(), whoknowsMode);

    }


    abstract WhoKnowsMode getWhoknowsMode(T params);

    abstract WrapperReturnNowPlaying generateWrapper(T params, WhoKnowsMode whoKnowsMode) throws LastFmException;

    public void generateWhoKnows(WrapperReturnNowPlaying wrapperReturnNowPlaying, T ap, long author, WhoKnowsMode effectiveMode) {
        wrapperReturnNowPlaying.getReturnNowPlayings()
                .forEach(x ->
                        x.setGenerateString(() -> {
                            String userString = getUserString(ap.getE(), x.getDiscordId());
                            x.setDiscordName(userString);
                            return ". " +
                                   "**[" + LinkUtils.cleanMarkdownCharacter(userString) + "](" +
                                   PrivacyUtils.getUrlTitle(x) +
                                   ")** - " +
                                   x.getPlayNumber() + " plays\n";
                        })
                );
        switch (effectiveMode) {

            case IMAGE, PIE -> {
                wrapperReturnNowPlaying.getReturnNowPlayings().stream().limit(15)
                        .forEach(x -> x.setDiscordName(CommandUtil.getUserInfoUnescaped(ap.getE(), x.getDiscordId()).username()));
                if (effectiveMode.equals(WhoKnowsMode.IMAGE)) {
                    doImage(ap, wrapperReturnNowPlaying);
                } else {
                    doPie(ap, wrapperReturnNowPlaying);
                }
            }
            case LIST -> doList(ap, wrapperReturnNowPlaying);
        }
    }

    BufferedImage doImage(T ap, WrapperReturnNowPlaying wrapperReturnNowPlaying) {
        Context e = ap.getE();

        BufferedImage logo = null;
        String title;
        if (e.isFromGuild()) {
            logo = CommandUtil.getLogo(db, e);
            title = e.getGuild().getName();
        } else {
            title = e.getJDA().getSelfUser().getName();
        }
        BufferedImage image = WhoKnowsMaker.generateWhoKnows(wrapperReturnNowPlaying, EnumSet.allOf(WKMode.class), title, logo, ap.getE().getAuthor().getIdLong());
        sendImage(image, e);

        return logo;
    }

    void doList(T ap, WrapperReturnNowPlaying wrapperReturnNowPlaying) {
        EmbedBuilder embedBuilder = new ChuuEmbedBuilder(ap.getE());
        StringBuilder builder = new StringBuilder();

        Context e = ap.getE();

        int counter = 1;
        for (ReturnNowPlaying returnNowPlaying : wrapperReturnNowPlaying.getReturnNowPlayings()) {
            builder.append(counter++)
                    .append(returnNowPlaying.toString());
            if (counter == 11)
                break;
        }
        String usable;
        if (e.isFromGuild()) {
            usable = CommandUtil.escapeMarkdown(e.getGuild().getName());
        } else {
            usable = e.getJDA().getSelfUser().getName();
        }
        embedBuilder.setTitle(getTitle(ap, usable)).
                setThumbnail(CommandUtil.noImageUrl(wrapperReturnNowPlaying.getUrl())).setDescription(builder);
        e.sendMessage(embedBuilder.build())
                .queue(message1 ->
                        new Reactionary<>(wrapperReturnNowPlaying
                                .getReturnNowPlayings(), message1, embedBuilder));
    }

    void doPie(T ap, WrapperReturnNowPlaying returnNowPlaying) {
        PieChart pieChart = this.pie.doPie(ap, returnNowPlaying.getReturnNowPlayings());
        String usable;
        Context e = ap.getE();
        if (e.isFromGuild()) {
            usable = CommandUtil.escapeMarkdown(e.getGuild().getName());
        } else {
            usable = e.getJDA().getSelfUser().getName();
        }
        pieChart.setTitle(getTitle(ap, usable));
        BufferedImage bufferedImage = new BufferedImage(1000, 750, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufferedImage.createGraphics();
        GraphicUtils.setQuality(g);

        pieChart.paint(g, 1000, 750);
        BufferedImage image = GraphicUtils.getImage(returnNowPlaying.getUrl());
        if (image != null) {
            BufferedImage backgroundImage = Scalr.resize(image, 150);
            g.drawImage(backgroundImage, 10, 750 - 10 - backgroundImage.getHeight(), null);
        }
        sendImage(bufferedImage, e);
    }

    public abstract String getTitle(T params, String baseTitle);
}
