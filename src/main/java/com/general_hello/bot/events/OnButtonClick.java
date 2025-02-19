package com.general_hello.bot.events;

import com.general_hello.Bot;
import com.general_hello.Config;
import com.general_hello.bot.commands.DashboardCommand;
import com.general_hello.bot.commands.LeaderboardCommand;
import com.general_hello.bot.database.DataUtils;
import com.general_hello.bot.objects.Game;
import com.general_hello.bot.objects.GlobalVariables;
import com.general_hello.bot.objects.SpecialPost;
import com.general_hello.bot.utils.JsonUtils;
import com.general_hello.bot.utils.OddsGetter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("ConstantConditions")
public class OnButtonClick extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnButtonClick.class);
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        // users can spoof this id so be careful what you do with this
        String[] id = event.getComponentId().split(":"); // this is the custom id we specified in our button
        String authorId = id[0];

        if (id.length == 1) {
            return;
        }

        String type = id[1];

        // When storing state like this is it is highly recommended doing some kind of verification that it was generated by you, for instance a signature or local cache
        if (!authorId.equals("0000") && !authorId.equals(event.getUser().getId())) {
            event.reply("You can't press this button").setEphemeral(true).queue();
            return;
        }

        User author = event.getUser();
        if (!DataUtils.hasAccount(author.getIdLong())) {
            event.reply("Kindly make an account first by using `/register`").setEphemeral(true).queue();
            return;
        }

        if (DataUtils.isBanned(author.getIdLong())) {
            event.reply("You are banned from using the bot.").setEphemeral(true).queue();
            return;
        }

        if ("bet".equals(type)) {
            String teamName = id[2];
            String gameId = id[3];
            if (DataUtils.newBet(event.getUser().getIdLong(), teamName, gameId)) {
                JsonUtils.incrementInteraction(OddsGetter.gameIdToGame.get(gameId).getSportType().getName(), author.getId());
                event.reply("You have predicted " + teamName + " will win the game.").setEphemeral(true).queue();
            } else {
                event.reply("You have already placed a prediction on " + teamName + ".").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        String value = event.getSelectedOptions().get(0).getValue();

        switch (value) {
            case "shutdown":
                DashboardCommand.LAST_USED.setShutdown(Instant.now().getEpochSecond() * 1000);
                DashboardCommand.buildAndSend(event.getChannel().asTextChannel());
                for (Game game : OddsGetter.games) {
                    try {
                        long channelId = game.getSportType().getChannelId();
                        long messageId = OddsGetter.gameIdToMessageId.get(game.getId());
                        event.getJDA().getTextChannelById(channelId).deleteMessageById(messageId).queue();
                        LOGGER.info("Deleted an unfinished game message in channel " + channelId + " with message id " + messageId);
                    } catch (Exception ignored) {}
                }
                if (DashboardCommand.deleteDashboard(event.getChannel().asTextChannel())) {
                    LOGGER.info("Deleted the dashboard successfully.");
                } else {
                    LOGGER.warn("Failure to delete the dashboard.");
                }
                event.reply("""
                        **Warning:**
                        :warning: Games that hasn't been finished may cause unexpected issues.
                        :warning: Dashboard won't work due to it being deleted.
                        :warning: Bot will shut down in 5 seconds.
                        
                        **Shut down successfully!**""").setEphemeral(true).queue();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                event.getJDA().shutdown();
                LOGGER.info("Bot shut down by " + event.getUser().getAsTag());
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.exit(0);
                break;
            case "reloaddata":
                try {
                    DashboardCommand.LAST_USED.setReloadData(Instant.now().getEpochSecond() * 1000);
                    DashboardCommand.buildAndSend(event.getChannel().asTextChannel());
                    event.reply("Reloading the data... Check the logs for more details").setEphemeral(true).queue();
                    new OddsGetter.GetOddsTask().run();
                }  catch (Exception ignored) {}
                break;
            case "hof":
                DashboardCommand.LAST_USED.setHof(Instant.now().getEpochSecond() * 1000);
                DashboardCommand.buildAndSend(event.getChannel().asTextChannel());
                List<SpecialPost> specialPosts = DataUtils.getSpecialPosts();
                Collections.sort(specialPosts);
                Collections.reverse(specialPosts);
                if (specialPosts.isEmpty()) {
                    event.reply("No special posts yet").setEphemeral(true).queue();
                    return;
                }
                SpecialPost topPost = specialPosts.get(0);
                DataUtils.setTopPost(topPost.getUnixTime());
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(Color.orange);
                String posterId = DataUtils.getPoster(topPost.getUnixTime());
                User userById = event.getJDA().getUserById(posterId);
                embedBuilder.setAuthor(userById.getName(), null, userById.getEffectiveAvatarUrl());
                embedBuilder.setDescription(DataUtils.getContent(topPost.getUnixTime()));
                event.getGuild().getTextChannelById(GlobalVariables.HALL_OF_FAME).sendMessageEmbeds(embedBuilder.build()).queue();
                event.reply("Successfully sent the Hall of Fame post").setEphemeral(true).queue();
                break;
            case "extractdata":
                DashboardCommand.LAST_USED.setExtractData(Instant.now().getEpochSecond() * 1000);
                DashboardCommand.buildAndSend(event.getChannel().asTextChannel());
                JSONArray finalArray = new JSONArray();
                List<Long> champs = DataUtils.getChamps();
                if (champs != null) {
                    for (long champ : champs) {
                        User champUser = event.getJDA().getUserById(champ);
                        long champTimeWeeks = DataUtils.getChampTime(champ);
                        String champTime;
                        if (champTimeWeeks == -1) {
                            champTime = "Bot wasn't online when the champ was assigned";
                        } else {
                            champTime = String.format("%d week(s)", ((int) ((Instant.now().getEpochSecond() * 1000) - champTimeWeeks) / 604800));
                        }
                        JSONObject champObject = new JSONObject();
                        champObject.put("champName", champUser.getAsTag());
                        champObject.put("userId", champ);
                        champObject.put("champWallet", DataUtils.getSolanaAddress(champ));
                        champObject.put("hofCount", DataUtils.getHofCount(champ));
                        champObject.put("highInteractionPosts", DataUtils.getHighInteractionPosts(champ));
                        champObject.put("followerCount", DataUtils.getFollowersOfChamp(champ).size());
                        champObject.put("champWeeks", champTime);
                        finalArray.put(champObject);
                    }

                    //Write JSON file
                    long time = System.currentTimeMillis();
                    try (FileWriter file = new FileWriter("champs-" + time + ".json")) {
                        //We can write any JSONArray or JSONObject instance to the file
                        file.write(finalArray.toString());
                        file.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    File file = new File("champs-" + time + ".json");
                    event.replyFile(file, "champs-" + time + ".json").setEphemeral(true).queue();
                    LOGGER.info("Made a new champs-" + time + ".json file");
                } else {
                    LOGGER.info("No champs found");
                    event.reply("No champs found").setEphemeral(true).queue();
                }
                break;
            case "restartlb":
                OnReadyEvent.dailyLbTask.shutdownNow();
                OnReadyEvent.dailyLbTask = Executors.newScheduledThreadPool(1);
                OnReadyEvent.dailyLbTask.scheduleAtFixedRate(() -> {
                    LeaderboardCommand.wins = new HashMap<>();
                    LeaderboardCommand.loss = new HashMap<>();
                    LOGGER.info("Successfully cleared the leaderboard daily data.");
                }, 0, 1, TimeUnit.DAYS);
                event.reply("Done resetting the task").setEphemeral(true).queue();
                break;
        }

        if (event.getSelectMenu().getId().equals("menu:winning")) {
            String[] split = event.getSelectedOptions().get(0).getValue().split(":");
            String gameId = split[0];
            String teamName = split[1];
            if (!OddsGetter.gameIdToGame.containsKey(gameId)) {
                event.reply("Invalid game id.").setEphemeral(true).queue();
                return;
            }
            Game game = OddsGetter.gameIdToGame.get(gameId);
            ArrayList<String> teams = new ArrayList<>(Arrays.asList(game.getHomeTeam(), game.getAwayTeam()));
            DataUtils.getUsers(gameId).forEach(userID -> {
                String betTeam = DataUtils.getBetTeam(userID, gameId);
                if (betTeam.equals(teamName)) {
                    JsonUtils.incrementCorrectPred(game.getSportType().getName(), userID);
                    DataUtils.addWin(userID);
                    int wins = LeaderboardCommand.wins.getOrDefault(userID, 0);
                    LeaderboardCommand.wins.put(userID, wins + 1);
                    DataUtils.addResult(userID, "W");
                } else {
                    JsonUtils.incrementWrongPred(game.getSportType().getName(), userID);
                    DataUtils.addLose(userID);
                    int loss = LeaderboardCommand.loss.getOrDefault(userID, 0);
                    LeaderboardCommand.loss.put(userID, loss + 1);
                    DataUtils.addResult(userID, "L");
                }
            });

            EmbedBuilder embed = OddsGetter.getEmbedPersonal(game);
            teams.remove(teamName);
            long textChannelId = game.getSportType().getChannelId();
            MessageChannel channel = Bot.getJda().getTextChannelById(textChannelId);
            long messageId = OddsGetter.gameIdToMessageId.get(gameId);
            channel.retrieveMessageById(messageId).queue((message) -> message.editMessageEmbeds(embed.build())
                    .setActionRow(net.dv8tion.jda.api.interactions.components.buttons.Button.success("winner", teamName).asDisabled(),
                            Button.danger("loser", teams.get(0)).asDisabled()).queue());

            event.reply("Successfully set the result of the game.").setEphemeral(true).queue();
            OddsGetter.gameIdToGame.remove(gameId);
            OddsGetter.games.remove(game);
            OddsGetter.gameIdToMessageId.remove(gameId);
        }
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        if (event.getGuild().getIdLong() != Config.getLong("guild")) {
            return;
        }

        AtomicBoolean isNewChamp = new AtomicBoolean(false);
        event.getRoles().forEach((role -> {
            if (role.getName().contains(Config.get("champ_prefix"))) {
                isNewChamp.set(true);
            }
        }));

        if (isNewChamp.get()) {
            DataUtils.newChampTime(System.currentTimeMillis() / 1000L, event.getUser().getIdLong());
            LOGGER.info("New champ " + event.getUser().getAsTag() + " added to the database");
        }
    }
}
