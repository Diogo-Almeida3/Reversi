package pt.isec.amov.reversi.game.jsonClasses

import pt.isec.amov.reversi.game.PieceMoves

class ServerMoveData {
    var move: PieceMoves? = null
    var currentPiece: Int? = null
    var type: String? = null

    constructor(move: PieceMoves, currentPiece: Int): super() {
        this.type = "SERVER_MOVE"
        this.move = move
        this.currentPiece = currentPiece
    }
}