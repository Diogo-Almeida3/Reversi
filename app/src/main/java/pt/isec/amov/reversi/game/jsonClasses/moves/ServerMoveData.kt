package pt.isec.amov.reversi.game.jsonClasses.moves

import pt.isec.amov.reversi.game.PieceMoves
import java.util.ArrayList

class ServerMoveData {
    var move: PieceMoves? = null
    var currentPiece: Int? = null
    var type: String? = null
    var exchangeArrayListX = ArrayList<Int>()
    var exchangeArrayListY = ArrayList<Int>()

    constructor(move: PieceMoves, currentPiece: Int): super() {
        this.type = "SERVER_MOVE"
        this.move = move
        this.currentPiece = currentPiece
    }

    constructor(move: PieceMoves, currentPiece: Int, exchangeArrayList: ArrayList<PieceMoves>) : super() {
        this.type = "SERVER_MOVE"
        this.move = move
        this.currentPiece = currentPiece
        for(i in 0 until exchangeArrayList.size){
            exchangeArrayListX.add(exchangeArrayList[i].getX())
            exchangeArrayListY.add(exchangeArrayList[i].getY())
        }
    }
}