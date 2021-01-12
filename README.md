To use the Bot just fill out the 'YOUR TOKEN HERE' part of the src/main/java/jdmusicbot/App.java source file. After inserting your own TOKEN just start the bot via 'gradle run'
```java
public static void main(String[] args) {
	try {
		 JDA jdabuild = new JDABuilder(AccountType.BOT)
			.setToken("YOUR TOKEN HERE")
			.addEventListeners(new App())
			.build();
		 jdabuild.awaitReady();
	} catch (LoginException | InterruptedException e) {
		e.printStackTrace();
	}
}
```
