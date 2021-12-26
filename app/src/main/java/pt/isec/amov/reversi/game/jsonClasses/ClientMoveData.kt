package pt.isec.amov.reversi.game.jsonClasses

import pt.isec.amov.reversi.game.PieceMoves
import java.util.ArrayList

class ClientMoveData {

    var move: PieceMoves? = null
    var currentPiece: Int? = null
    var type: String? = null

    constructor(move: PieceMoves, currentPiece: Int): super() {
        this.type = "CLIENT_MOVE"
        this.move = move
        this.currentPiece = currentPiece
    }
}