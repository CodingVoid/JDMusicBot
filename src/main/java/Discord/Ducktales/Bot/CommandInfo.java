package Discord.Ducktales.Bot;

public class CommandInfo {

	public Command cmd;
	public String description;
	public String usage;

	public CommandInfo(String usage, String description, Command cmd) {
		this.usage = usage;
		this.description = description;
		this.cmd = cmd;
	}
}
