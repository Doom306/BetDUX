package com.general_hello.bot.objects;

import com.general_hello.Config;

/**
 * <p>Make a <b>.env</b> file with the following contents and replace the <b>?</b> with the proper values</p>
 */
/*
    # Bot token
    TOKEN=[TOKEN HERE]
    # Your id
    OWNER_ID=[OWNER ID HERE]
    # Kinda useless (Prefix)
    PREFIX=/
    # Partner owner id
    OWNER_ID_PARTNER=[ANOTHER OWNER ID]
    # Server ID
    GUILD=[GUILD SERVER ID]
    # Link of the hyperlinks
    LINK=[https://discord.com]
    # Rapid API key (https://rapidapi.com/theoddsapi/api/live-sports-odds/)
    KEY=[API KEY]
    # Bookmaker to get the odds from
    BOOKMAKERS=BetMGM
    # Football Channel
    FOOTBALL=[CHANNEL ID]
    # Baseball Channel
    BASEBALL=[CHANNEL ID]
    # Cricket channel
    CRICKET=[CHANNEL ID]
    # Golf channel
    GOLF=[CHANNEL ID]
    # Hockey channel
    HOCKEY=[CHANNEL ID]
    # Mixed martial arts channel
    MIXED_ARTS=[CHANNEL ID]
    # Soccer channel
    SOCCER=[CHANNEL ID]
    # Tennis channel
    TENNIS=[CHANNEL ID]
    # Champ role prefix
    CHAMP_PREFIX=[ROLE PREFIX]
    # Hall of Fame channel id
    HALL_OF_FAME=[CHANNEL ID]
 */
public class GlobalVariables {
    public static final String API_KEY = Config.get("key");
    public static final String LINK = Config.get("link");
    public static final String BOOKMAKERS = Config.get("bookmakers");
    public static final String CHAMPS = Config.get("champ_prefix").toLowerCase();
    public static final String HALL_OF_FAME = Config.get("hall_of_fame");
    public static final String VERSION = "2.6.0";
    public static final String WIN_EMOJI = Config.get("win");
    public static final String LOSE_EMOJI = Config.get("lose");
}
