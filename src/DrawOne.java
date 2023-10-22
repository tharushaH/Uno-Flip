/**
 *  DrawOne is a subclass of TurnSequence used to handle the game sequences of a draw one in the game, Uno Flip
 *  by updating the game's state.
 *  @author Hubert Dang
 *  Date: 2023-10-22
 */
public class DrawOne extends TurnSequence {

    public DrawOne(Game game) {
        super(game);
    }

    /**
     * Executes the appropriate game sequence according to the card played by changing the game state.
     *
     * @param card The card that was played
     */
    @Override
    public void executeSequence(Card card) {
        game.setTopCard(card);
        game.setCurrentColour(card.getColour());
        game.setCurrentRank(card.getRank());
        game.drawNCards(1, game.getNextTurn());
        game.skipTurn();
    }
}
