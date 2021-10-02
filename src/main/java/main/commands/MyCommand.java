package main.commands;

import main.Chuu;
import main.parsers.Parser;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public abstract class MyCommand extends ListenerAdapter {
	boolean respondInPrivate = true;
	Parser parser;

	abstract String getDescription();

	String getUsageInstructions() {
		return parser.getUsage(getAliases().get(0));
	}

	abstract List<String> getAliases();

	/**
	 * @param e Because we are using the {@link main.commands.CustomInterfacedEventManager CustomInterfacedEventManager} we know that this is the only OnMessageReceived event handled so we can skip the cheks
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		//if (!e.getMessage().getContentRaw().startsWith(PREFIX) || (e.getAuthor().isBot() && !respondToBots()))
		//	return;

		//if (containsCommand(e.getMessage())) {
		e.getChannel().sendTyping().queue();
		System.out.println("We received a message from " +
				e.getAuthor().getName() + "; " + e.getMessage().getContentDisplay());
		if (!e.getChannelType().isGuild() && !respondInPrivate) {
			sendMessageQueue(e, "This command only works in a server");
			return;
		}
		measureTime(e);


	}

	void measureTime(MessageReceivedEvent e) {
		long startTime = System.currentTimeMillis();
		onCommand(e);
		long endTime = System.currentTimeMillis();
		long timeElapsed = endTime - startTime;
		System.out.println("Execution time in milliseconds " + getName() + " : " + timeElapsed);
		System.out.println();
	}

	abstract void onCommand(MessageReceivedEvent e);
	//}

	abstract String getName();

	void sendMessageQueue(MessageReceivedEvent e, String message) {
		sendMessageQueue(e, new MessageBuilder().append(message).build());
	}

	private void sendMessageQueue(MessageReceivedEvent e, Message message) {
		if (e.isFromType(ChannelType.PRIVATE))
			e.getPrivateChannel().sendMessage(message).queue();
		else
			e.getTextChannel().sendMessage(message).queue();
	}

	MessageAction sendMessage(MessageReceivedEvent e, String message) {
		return sendMessage(e, new MessageBuilder().append(message).build());
	}

	private MessageAction sendMessage(MessageReceivedEvent e, Message message) {
		if (e.isFromType(ChannelType.PRIVATE))
			return e.getPrivateChannel().sendMessage(message);
		else
			return e.getTextChannel().sendMessage(message);
	}

	String[] commandArgs(Message message) {
		return commandArgs(message.getContentDisplay());
	}

	private String[] commandArgs(String string) {
		return string.split("\\s+");
	}


	void sendImage(BufferedImage image, MessageReceivedEvent e) {
		sendImage(image, e, false);
	}

	void sendImage(BufferedImage image, MessageReceivedEvent e, boolean makeSmaller) {
		//MessageBuilder messageBuilder = new MessageBuilder();
		if (image == null) {
			sendMessageQueue(e, "Ish Pc Bad");
			return;
		}
		ByteArrayOutputStream b = new ByteArrayOutputStream();

		try {
			String format = "png";
			if (makeSmaller)
				format = "jpg";
			ImageIO.write(image, format, b);

			byte[] img = b.toByteArray();
			if (img.length < 8388608)
				e.getChannel().sendFile(img, "cat." + format).queue();
				//messageBuilder.sendTo(e.getChannel()).addFile(img, "cat.png").queue();
			else
				e.getChannel().sendMessage("Boot too big").queue();
			//messageBuilder.setContent("Boot to big").sendTo(e.getChannel()).queue();

		} catch (IOException ex) {
			sendMessageQueue(e, "Ish Pc Bad");
			Chuu.getLogger().warn(ex.getMessage(), ex);
		}


	}

	String getUserStringConsideringGuildOrNot(MessageReceivedEvent e, long who, String replacement) {
		String firstReturn;
		if ((firstReturn = getUserString(who, e, replacement)) == null) {
			return getUserGlobalString(who, e, replacement);
		} else return firstReturn;
	}

	public String getUserString(Long discordId, MessageReceivedEvent e, String replacement) {

		if (e.getChannelType().isGuild()) {
			Member member = e.getGuild().getMemberById(discordId);
			return member == null ? replacement : member.getEffectiveName();
		}
		return null;
	}

	String getUserGlobalString(Long discordId, MessageReceivedEvent e, String replacement) {
		try {
			User member = e.getJDA().retrieveUserById(discordId).complete();
			return member == null ? replacement : member.getName();
		} catch (Exception ex) {
			return replacement;
		}
	}
}