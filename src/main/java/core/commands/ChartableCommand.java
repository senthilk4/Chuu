package core.commands;

import core.commands.util.IPieable;
import core.commands.util.PieableChart;
import core.exceptions.InstanceNotFoundException;
import core.exceptions.LastFmException;
import core.imagerenderer.ChartQuality;
import core.imagerenderer.CollageMaker;
import core.imagerenderer.GraphicUtils;
import core.otherlisteners.Reactionary;
import core.parsers.ChartableParser;
import core.parsers.params.ChartParameters;
import dao.ChuuService;
import dao.entities.CountWrapper;
import dao.entities.DiscordUserDisplay;
import dao.entities.UrlCapsule;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.knowm.xchart.PieChart;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

public abstract class ChartableCommand<T extends ChartParameters> extends ConcurrentCommand<T> {
    public IPieable<UrlCapsule, ChartParameters> pie;
    public ChartableCommand(ChuuService dao) {
        super(dao);
        pie = new PieableChart(this.parser);
    }


    public abstract ChartableParser<T> getParser();

    @Override
    void onCommand(MessageReceivedEvent e) throws LastFmException, InstanceNotFoundException {
        T chartParameters = parser.parse(e);
        if (chartParameters == null)
            return;
        CountWrapper<BlockingQueue<UrlCapsule>> countWrapper = processQueue(chartParameters);
        BlockingQueue<UrlCapsule> urlCapsules = countWrapper.getResult();
        if (urlCapsules.isEmpty()) {
            this.noElementsMessage(e, chartParameters);
            return;
        }
        if (chartParameters.isList() || chartParameters.isPieFormat()) {
            ArrayList<UrlCapsule> liste = new ArrayList<>(urlCapsules.size());
            urlCapsules.drainTo(liste);
            if (chartParameters.isPieFormat()) {
                PieChart pieChart = pie.doPie(chartParameters, liste);
                doPie(pieChart, chartParameters, countWrapper.getRows());
            } else {
                doList(liste, chartParameters, countWrapper.getRows());
            }
        } else {
            doImage(urlCapsules, chartParameters.getX(), chartParameters.getY(), chartParameters);
        }
    }


    public abstract CountWrapper<BlockingQueue<UrlCapsule>> processQueue(T params) throws
            LastFmException;

    void generateImage(BlockingQueue<UrlCapsule> queue, int x, int y, MessageReceivedEvent e) {
        int size = queue.size();
        ChartQuality chartQuality = ChartQuality.PNG_BIG;
        int minx = (int) Math.ceil((double) size / x);
        if (minx == 1)
            x = size;
        if (size > 45 && size < 400)
            chartQuality = ChartQuality.JPEG_BIG;
        else if (size >= 400)
            chartQuality = ChartQuality.JPEG_SMALL;
        BufferedImage image = CollageMaker
                .generateCollageThreaded(x, minx, queue, chartQuality);
        sendImage(image, e, chartQuality);
    }


    public void doImage(BlockingQueue<UrlCapsule> queue, int x, int y, T parameters) {
        CompletableFuture<Message> future = null;
        MessageReceivedEvent e = parameters.getE();
        if (x * y > 100) {
            future = e.getChannel().sendMessage("Going to take a while").submit();
        }
        generateImage(queue, x, y, e);
        CommandUtil.handleConditionalMessage(future);
    }


    public void doList(List<UrlCapsule> urlCapsules, T params, int count) {

        StringBuilder a = new StringBuilder();
        for (int i = 0; i < 10 && i < urlCapsules.size(); i++) {
            a.append(i + 1).append(urlCapsules.get(i).toEmbedDisplay());
        }
        DiscordUserDisplay userInfoConsideringGuildOrNot = CommandUtil.getUserInfoConsideringGuildOrNot(params.getE(), params.getDiscordId());

        EmbedBuilder embedBuilder = configEmbed(new EmbedBuilder()
                .setDescription(a)
                .setColor(CommandUtil.randomColor())
                .setThumbnail(userInfoConsideringGuildOrNot.getUrlImage()), params, count);
        MessageBuilder mes = new MessageBuilder();
        params.getE().getChannel().sendMessage(mes.setEmbed(embedBuilder.build()).build()).queue(message1 ->
                new Reactionary<>(urlCapsules, message1, embedBuilder));
    }

    public void doPie(PieChart pieChart, T chartParameters, int count) {
        DiscordUserDisplay userInfoNotStripped = CommandUtil.getUserInfoNotStripped(chartParameters.getE(), chartParameters.getDiscordId());
        String subtitle = configPieChart(pieChart, chartParameters, count, userInfoNotStripped.getUsername());
        String urlImage = userInfoNotStripped.getUrlImage();
        BufferedImage bufferedImage = new BufferedImage(1000, 750, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufferedImage.createGraphics();
        GraphicUtils.setQuality(g);
        Font annotationsFont = pieChart.getStyler().getAnnotationsFont();
        pieChart.paint(g, 1000, 750);
        g.setFont(annotationsFont.deriveFont(11.0f));
        Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(subtitle, g);
        g.drawString(subtitle, 1000 - 10 - (int) stringBounds.getWidth(), 740 - 2);
        GraphicUtils.inserArtistImage(urlImage, g);
        sendImage(bufferedImage, chartParameters.getE());
    }


    public abstract EmbedBuilder configEmbed(EmbedBuilder embedBuilder, T params, int count);

    public abstract String configPieChart(PieChart pieChart, T params, int count, String initTitle);


    public abstract void noElementsMessage(MessageReceivedEvent e, T parameters);


}