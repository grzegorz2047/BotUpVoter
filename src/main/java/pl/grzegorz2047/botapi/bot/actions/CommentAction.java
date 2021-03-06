package pl.grzegorz2047.botapi.bot.actions;

import eu.bittrade.libs.steemj.SteemJ;
import eu.bittrade.libs.steemj.apis.database.models.state.Discussion;
import eu.bittrade.libs.steemj.base.models.AccountName;
import eu.bittrade.libs.steemj.base.models.Permlink;
import eu.bittrade.libs.steemj.base.models.VoteState;
import eu.bittrade.libs.steemj.base.models.operations.CommentOperation;
import eu.bittrade.libs.steemj.exceptions.SteemCommunicationException;
import eu.bittrade.libs.steemj.exceptions.SteemInvalidTransactionException;
import eu.bittrade.libs.steemj.exceptions.SteemResponseException;
import pl.grzegorz2047.botapi.bot.actions.exceptions.InsufficentArgumensToActException;
import pl.grzegorz2047.botapi.bot.argument.Argument;
import pl.grzegorz2047.botapi.bot.interfaces.BotAction;
import pl.grzegorz2047.botapi.bot.interfaces.BotRule;

import java.util.*;

public class CommentAction implements BotAction {


    private HashMap<String, BotRule> botRules = new HashMap<>();
    private ActionProcessor actionProcessor = new ActionProcessor();

    public CommentAction() {
    }


    @Override
    public boolean act(SteemJ steemJ, HashMap<String, Argument> arguments) throws SteemResponseException, SteemCommunicationException, SteemInvalidTransactionException, InsufficentArgumensToActException {
        System.out.println("Im about to comment!");
        String message = arguments.get("commentMessage").asString();
        String authorToCommentOn = arguments.get("userAccount").asString();
        if (message == null) {
            return false;
        }
        if (authorToCommentOn == null) {
            return false;
        }
        AccountName userAccount = new AccountName(authorToCommentOn);
        Permlink permlink = new Permlink(arguments.get("permlink").asString());
        String commentTagsArg = arguments.get("votingTags").asString();
        if (commentTagsArg == null) {
            return false;
        }

        if (!arguments.keySet().containsAll(getRequiredKeyProperties())) {
            throw new InsufficentArgumensToActException("You dont have all required arguments to act! Current keys: " + Arrays.toString(arguments.keySet().toArray()) + " requirements are " + Arrays.toString(getRequiredKeyProperties().toArray()));
        }
        Argument permlinkArg = arguments.get("permlink");
        Argument userAccountArg = arguments.get("userAccount");
        Argument votingStrengthArg = arguments.get("votingStrength");
        Argument botNameArg = arguments.get("botName");
        AccountName botAccount = new AccountName(botNameArg.asString());
        if (permlinkArg == null || userAccountArg == null || votingStrengthArg == null) {
            throw new InsufficentArgumensToActException("Arguments contain null!");
        }

        HashMap<String, Argument> actionArguments = new HashMap<>(arguments);
        Discussion content = steemJ.getContent(new AccountName(userAccountArg.asString()), new Permlink(permlinkArg.asString()));

        List<VoteState> activeVotes = content.getActiveVotes();

        boolean botOnVoterList = actionProcessor.isBotOnVoterList(activeVotes, botAccount.getName());
        actionArguments.put("votedBefore", new Argument(botOnVoterList));
        if (!actionProcessor.canProceed(actionArguments, botRules.values())) {
            System.out.println("Rules broken. Cant proceed!");
            return false;
        }
        String[] commentTags = commentTagsArg.split(",");
        System.out.println(message + ", " + authorToCommentOn + ", " + permlink.getLink() + ", " + commentTagsArg);
        CommentOperation comment = steemJ.createComment(botAccount, userAccount, permlink, message, commentTags);
        System.out.println("Successfuly commented!");
        return true;
    }


    @Override
    public boolean addRule(String name, BotRule rule) {
        if (botRules.containsKey(name)) {
            return false;
        }
        botRules.put(name, rule);
        return true;
    }

    @Override
    public boolean removeRule(String name) {
        if (botRules.containsKey(name)) {
            botRules.remove(name);
            return true;
        }
        return false;
    }

    @Override
    public LinkedList<String> getRequiredKeyProperties() {
        LinkedList<String> finalRequirements = new LinkedList<>(Arrays.asList("commentMessage", "votingTags", "botName"));
        for (Map.Entry<String, BotRule> botRule : botRules.entrySet()) {
            LinkedList<String> requiredKeyProperties = botRule.getValue().getRequiredKeyProperties();
            finalRequirements.removeAll(requiredKeyProperties);
            finalRequirements.addAll(requiredKeyProperties);

        }
        return finalRequirements;
    }

    @Override
    public LinkedList<String> getRequiredRuntimeKeyProperties() {
        LinkedList<String> finalRequirements = new LinkedList<>(Arrays.asList("permlink", "userAccount"));
        for (Map.Entry<String, BotRule> botRule : botRules.entrySet()) {
            LinkedList<String> requiredKeyProperties = botRule.getValue().getRequiredRuntimeKeyProperties();
            finalRequirements.removeAll(requiredKeyProperties);
            finalRequirements.addAll(requiredKeyProperties);
        }
        return finalRequirements;
    }

    @Override
    public Set<String> printAllRules() {
        return botRules.keySet();
    }

    @Override
    public String toString() {
        return "rules=" + this.printAllRules() + ", " + "requiredKeyProperties=" + Arrays.toString(getRequiredKeyProperties().toArray());
    }

}
