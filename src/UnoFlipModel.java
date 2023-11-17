import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
/**
 * The Game class represent a game of Uno Flip. Uno Flip can be played with 2-4 players.
 * This class initializes and manages a game of Uno Flip by managing player turns, displaying
 * the player's cards, validating card placement, scoring and game logic based on what cards were played.
 *
 *
 * @author  Amilesh Nanthakumaran
 * Date: 2023-10-20
 */
public class UnoFlipModel {

    private boolean turnFinished; //true if user has played/drawn a card, else false
    private boolean skipTurn; //true if skipping the next person, else false
    private boolean challenge; //true if next player wants to challenge, false if they do not
    private boolean dontAsk;
    private boolean turnDirection; //true is clockwise(1->2->3->4), false is counterclockwise(1->4->3->2)
    private int numPlayers; //number of players in the game
    private int chosenCardIndex; //user inputted card index
    private int currentTurn; // 0 is player 1, 1 is player 2, 2 is player 3, 3 is player 4
    private int nextPlayerIndex; //index of next player
    private String status; //indicate the status for which the view will update to
    private Deck deck; //deck that will be used for the game
    private Card.Colour currentColour; //current colour of the game
    private Card.Rank currentRank; //current rank of the game
    private Card topCard;//top card that is in play
    private List<UnoFlipView> views;  // list of views
    private ArrayList<TurnSequence> turnSeqs; // arraylist of turn sequences
    private ArrayList<Player> players; //arraylist of players


    public static final int DRAW_ONE_BUTTON = -1; // draw one button indicator

    //Constants used for Turn sequence
    public static final int TURN_SEQ_SELF_DRAW_ONE = 14;

    //Constants used to indicate the current status
    public static final String STATUS_CHALLENGE_MESSAGE  = "THE NEXT PLAYER HAS THE OPTION TO CHALLENGE";
    public static final String STATUS_STANDARD = " ";
    public static final String STATUS_CHALLENGE_INNOCENT = "INNOCENT: NEXT PLAYER DRAWS 4 CARDS";
    public static final String STATUS_CHALLENGE_GUILTY = "GUILTY:YOU DRAW 2 CARDS";
    public static final String STATUS_PLAYABLE_CARD = "YOU HAVE PLAYABLE CARD";
    public static final String STATUS_INVALID_CARD_BEING_PLACED = "THE CARD YOU PLACED DOES NOT MATCH THE TOP CARD. TRY AGAIN";
    public static final String STATUS_PLAYER_SKIPPING_TURN = "CANNOT SKIP A TURN, EITHER PLAY A CARD FROM THE HAND OR DRAW FROM THE DECK";
    public static final String STATUS_TURN_FINISHED = "YOUR TURN IS FINISHED, PRESS NEXT PLAYER";
    public static final String STATUS_DONE = "done";
    
    /**
     * Constructs a new game of Uno by initializing fields with default settings.
     */
    public UnoFlipModel(){
        this.players = new ArrayList<Player>(); //empty player list
        this.turnDirection = true; //initialize to clockwise
        this.currentTurn = 0; // start from player 1
        this.nextPlayerIndex = 1; // next player is player 2
        this.deck = new Deck(); //create a new deck
        this.currentColour = null; //set colour to null
        this.currentRank = null; //set rank to null
        this.numPlayers = 0; // initialize to 0
        this.chosenCardIndex = -1; //initialize to -1
        this.turnSeqs = new ArrayList<TurnSequence>(); //arraylist for turn sequences
        this.skipTurn = false; // initialize to false
        this.dontAsk = false;  // initialize to false
        for(int i =0;i<=8;i++){
            this.turnSeqs.add(new Number(this)); //Number
        }
        this.turnSeqs.add(new DrawOne(this)); //Draw_One
        this.turnSeqs.add(new Reverse(this)); //Reverse
        this.turnSeqs.add(new Skip(this)); //Skip
        this.turnSeqs.add(new Wild(this)); //Wild
        this.turnSeqs.add(new WildDrawTwo(this)); //Wild Draw Two
        this.turnSeqs.add(new SelfDrawOne(this)); //Self Draw One
        this.views = new ArrayList<UnoFlipView>(); //array list of views
    }


    /**
     * Returns the number of players, used for testing only
     * will only set the number of players if its between 2-4 players
     * @param numPlayers The number of players received from the UnoFLipController
     * @return the number of players that of the game
     */
    public int setNumPlayers(int numPlayers) {
        this.numPlayers = numPlayers;
        return this.numPlayers;
    }

    /**
     * Method addPlayers is meant to be activated by the UnoFlipController to initialize a player in the UnoFlip game
     * @param playerName - the name of the player that will be initialized
     */
    public void createPlayer(String playerName){
        Player p = new Player(playerName);
        addPlayer(p); //add player to arraylist
    }

    /**
     * Adds a player to the arraylist of players.
     * @param player The player to be added
     */
    public void addPlayer(Player player){
        this.players.add(player); // adding player to arraylist of players
    }

    /**
     * Method setUpInitialTopCard is meant to be called by the UnoFlipController to initialize the top card at the start of the game.
     */
    public void setUpInitialTopCard(){

        this.topCard = deck.takeCard();
        this.turnFinished = false;        //initialize false to allow first player to play/draw a card

        //continue to drawing a different card if WILD_DRAW_2
        while(this.topCard.getRank().ordinal() == Card.RANK_WILD_DRAW_2){
            this.deck.putCard(this.topCard);
            this.topCard = this.deck.takeCard();  //redraw the topCard
        }

        //if first card drawn form deck is an action card (non-number card)
        if(this.topCard.getRank().ordinal() > Card.RANK_NUMBER_CARDS) {
            this.turnSeqs.get(this.topCard.getRank().ordinal()).executeSequence(this.topCard); //execute sequence if action card

        //if number card drawn
        } else{
            this.currentColour = this.topCard.getColour();
            this.currentRank = this.topCard.getRank();
            this.status = STATUS_STANDARD;
            notifyViews();
        }
    }


    /**
     * Method notifyViews is meant to notify subscribers view about any changes that will affect the view of the UnoFlip Game
     */
    public void notifyViews(){

        //make sure there are views in the view arraylist to send UnoFlipEvents to.
        if(!this.views.isEmpty()){

            //sending events to the view to update depending on different situations that occur within the game
            //if wild draw 2 card and next player declines to challenge
            if(this.topCard.isWild() && !this.status.equals(STATUS_CHALLENGE_INNOCENT) && !this.status.equals(STATUS_CHALLENGE_GUILTY) && !this.dontAsk){

                this.status = this.currentColour.toString(); //set status as the current colour chosen by the player (ex: RED)

                for( UnoFlipView view: this.views ) {
                    view.handleUnoFlipStatusUpdate( new UnoFlipEvent(this, getCurrentPlayer().getName(), this.topCard.toString(), getCurrentPlayer().toString(),this.status,(this.currentRank == Card.Rank.WILD || this.currentRank == Card.Rank.WILD_DRAW_2)));
                }

            } else {

                for( UnoFlipView view: this.views ) {
                    view.handleUnoFlipStatusUpdate( new UnoFlipEvent(this, getCurrentPlayer().getName(), this.topCard.toString(), getCurrentPlayer().toString(),this.status ,this.currentRank == Card.Rank.WILD ));
                }
            }
        }
    }


    /**
     * PlayTurn method is used to handle game logic request sent by UnoFlipController for when a card is placed by the player
     * or when the player draws a card.
     * @param btnIndex - the index of the cards that is being played, -1 if player draws a card
     */
    public void playTurn(int btnIndex){

        //if turn is false (player has not played/drawn a card) allow player to play a card
        if (!this.turnFinished) {
            this.chosenCardIndex = btnIndex;

            //if player clicks on the draw one button
            if (this.chosenCardIndex == DRAW_ONE_BUTTON) {

                //if player does not have a valid card to play
                if (validSelfDrawOne()){
                    this.turnSeqs.get(TURN_SEQ_SELF_DRAW_ONE).executeSequence(null);
                    this.status = STATUS_STANDARD;
                    notifyViews();
                    this.turnFinished = true;

                //if player has a playable card
                } else{
                    this.status = STATUS_PLAYABLE_CARD;
                    notifyViews();
                }
                return;
            }

            int rank = getCurrentPlayer().getCard(this.chosenCardIndex).getRank().ordinal();

            //if the card wanting to be placed is a Wild Draw 2
            if (rank == Card.RANK_WILD_DRAW_2){
                this.turnSeqs.get(rank).executeSequence(getCurrentPlayer().playCard(this.chosenCardIndex));
                this.turnFinished = true;

            //if valid card
            } else if (this.turnSeqs.get(rank).isValid(getCurrentPlayer().getCard(this.chosenCardIndex))) {
                Card playCard = getCurrentPlayer().playCard(this.chosenCardIndex);

                //check if winner
                if (isWinner(getCurrentPlayer())) {
                    return;
                }

                this.turnSeqs.get(rank).executeSequence(playCard);
                this.status = STATUS_STANDARD;
                notifyViews();
                this.turnFinished = true;

            //if an invalid card
            } else {
                this.status = STATUS_INVALID_CARD_BEING_PLACED;
                notifyViews();
            }
        } else {
           this.status = STATUS_TURN_FINISHED;
            notifyViews();

        }
    }


    /**
     * Check if the player has no cards remaining
     * If player has no remaining cards, will update the players score
     * @param player The player that is checked
     * @return True if the player has no cards, false otherwise
     */
    private boolean isWinner(Player player){
        if (player.getHandSize() == 0) {
            getCurrentPlayer().setPlayerScore(getWinnerScore());
            this.status = "WINNER:" + getCurrentPlayer().getName() + " HAS WON !"; // (EX. "WINNER: Player 1 HAS WON!")
            notifyViews();
            return true;

        } else {
            return false;
        }
    }

    /**
     * Draw the amount of cards(n) based on which player(index) will be receiving them
     * @param n The amount of cards to be added to the hand of the player
     * @param index The index of the player that will be receiving cards
     */
    public void drawNCards(int n,int index){
        this.players.get(index).addCardToHand(n);
    }

    /**
     * Go to the turn of the next player based on turn direction
     */
    public void nextTurn() {

        //if the turn is finished, allow player to press next player
        if (this.turnFinished) {

            //clockwise (ex. 0->1->2->3)
            if (this.turnDirection) {
                this.currentTurn = (this.currentTurn + 1) % this.numPlayers;
                this.nextPlayerIndex = (this.currentTurn + 1) % this.numPlayers;

            //counterclockwise (ex. 0->3->2->1)
            } else {
                this.currentTurn = (this.currentTurn - 1 + this.numPlayers) % this.numPlayers;
                this.nextPlayerIndex = (this.currentTurn - 1 + this.numPlayers) % this.numPlayers;
            }

            //if next player's turn is being skipped (ex. player 2 is being skipped:  0->1->3->0)
            if (this.skipTurn){
                //clockwise (ex. 0->1->2->3)
                if (this.turnDirection) {
                    this.currentTurn = (this.currentTurn + 1) % this.numPlayers;
                    this. nextPlayerIndex = (this.currentTurn + 1) % this.numPlayers;

                //counterclockwise (ex. 0->3->2->1)
                } else {
                    this.currentTurn = (this.currentTurn - 1 + this.numPlayers) % this.numPlayers;
                    this.nextPlayerIndex = (this.currentTurn - 1 + this.numPlayers) % this.numPlayers;
                }
                this.skipTurn = false;
            }
            this.status = STATUS_STANDARD;
            notifyViews();
            this.turnFinished = false; // reset for next player

        // if player tries to skip turn
        } else {
            this.status = STATUS_PLAYER_SKIPPING_TURN;
            notifyViews();
        }

    }

    /**
     * Returns the score of the winner, score is generated based on the cards opponents are left holding
     * @return The score of the winner
     */
    private int getWinnerScore(){
        int winnerScore = 0;

        //handling if the last card played is a draw card (ex. RED_DRAW_ONE or WILD_DRAW_2)
        //cards must still be given to next players before winner's score is calculated
        if(this.topCard.getRank().ordinal() == Card.RANK_DRAW_ONE){
            drawNCards(1,this.nextPlayerIndex);
        } else if(this.topCard.getRank().ordinal() == Card.RANK_WILD_DRAW_2){
            drawNCards(2,this.nextPlayerIndex);
        }

        //accumulate points
        for(Player p: this.players){
            winnerScore += p.getHandScore();
        }
        return winnerScore;
    }

    /**
     * Checks to see if there is a playable card in hand before allowing player to draw a card from the deck
     * @return return true if valid to draw a card from deck, otherwise false.
     */
    private boolean validSelfDrawOne(){
        for (int i = 0; i < this.getCurrentPlayer().getHandSize();i++){
            if (this.getCurrentPlayer().getCard(i).getRank() == this.getCurrentRank() || this.getCurrentPlayer().getCard(i).getColour() == this.getCurrentColour()){
                return false;
            }
        }
        return true;
    }

    /**
     * Skip the turn of the next player
     */
    public void skipTurn(){
        this.skipTurn = true;
    }

    /**
     * Flips the direction of the game.
     */
    public void flipTurnDirection(){
        this.turnDirection = !this.turnDirection;
    }


    /**
     * Method addUnoFlipView adds a view to the view list
     * @param view - the view that will be added to the list
     */
    public void addUnoFlipView(UnoFlipView view){
        this.views.add(view);
    }

    /**
     * Method removeUnoFlipView removes view from the view list
     * @param view - the view that will be removed from the list
     */
    public void removeUnoFlipView(UnoFlipView view){
        this.views.remove(view);
    }


    /**
     * Gets the current colour of the game.
     * @return The current colour
     */
    public Card.Colour getCurrentColour() {
        return this.currentColour;
    }

    /**
     * Gets the current rank of the game.
     * @return The current rank
     */
    public Card.Rank getCurrentRank() {
        return this.currentRank;
    }

    /**
     * Gets the index of the player whose turn it is.
     * @return The index of the current player.
     */
    public int getCurrentTurn() {
        return this.currentTurn;
    }

    /**
     * Gets the current player.
     * @return The current player.
     */
    public Player getCurrentPlayer(){
        return this.players.get(this.currentTurn);
    }

    /**
     * Gets the index of the next player.
     * @return The index of the next player.
     */
    public int getNextTurn(){
        return this.nextPlayerIndex;
    }

    /**
     * Returns an ArrayList of players in the current game,used for testing only
     * @return The ArrayList of players
     */
    public ArrayList<Player> getPlayers() {
        return this.players;
    }

    /**
     * Returns the turn direction,used for testing only
     * @return The turn direction
     */
    public boolean getTurnDirection() {
        return this.turnDirection;
    }

    /**
     * Returns the number of players,used for testing only
     * @return The number of players
     */
    public int getNumPlayers() {
        return this.numPlayers;
    }

    /**
     * Returns the chosen card index of the user,used for testing only
     * @return The card index chosen by the user
     */
    public int getChosenCardIndex(){
        return this.chosenCardIndex;
    }

    /**
     * Returns the top card in play,used for testing only
     * @return The top card
     */
    public Card getTopCard(){
        return this.topCard;
    }

    /**
     * Return the ArrayList of sequences, for testing.
     * @return return a list of the sequences.
     */
    public ArrayList<TurnSequence> getTurnSeqs() {
        return this.turnSeqs;
    }

    /**
     * Gets the status of the challenge, whether the next player wants to challenge
     * @return true if next player wants to challenge, false if next player does not want to challenge
     */
    public boolean getChallenge(){
        return this.challenge;
    }

    /**
     * Setting the status
     * @param status - the new status
     */
    public void setStatus(String status ){
        this.status = status;
    }

    /**
     * Sets the current rank of the game.
     * @param currentRank The rank to be set
     */
    public void setCurrentRank(Card.Rank currentRank) {
        this.currentRank = currentRank;
    }

    /**
     * Sets the top card of the game.
     * @param topCard The card to be set as the top card.
     */
    public void setTopCard(Card topCard) {
        this.topCard = topCard;
    }

    /**
     * Set the current colour of the game
     * @param colour The colour to be set as the current colour
     */
    public void setCurrentColour(Card.Colour colour){
        this.currentColour = colour;
    }

    /**
     * Sets the challenge status for if the next player wants to challenge
     * @param challenge - next player's decision on challenging
     */
    public void setChallenge(boolean challenge){
        this.challenge = challenge;
    }

    /**
     * Set boolean for dontAsk
     * @param dontAsk set the ask permission
     */
    public void setDontAsk(boolean dontAsk) {
        this.dontAsk = dontAsk;
    }

}
