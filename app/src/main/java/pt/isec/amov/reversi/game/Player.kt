package pt.isec.amov.reversi.game

import android.graphics.Color

class Player(playerNumber: Int, initialPieces: Int, playerColor: Int) {

    private var pieces = initialPieces
    private var pieceType = playerNumber
    private var color = playerColor

    fun getPieces(): Int {
        return pieces
    }

    fun getColor() : Int {
        return color
    }

    fun getPieceType() : Int {
        return pieceType
    }

    fun setPieces(pieces : Int){
        this.pieces = pieces
    }
}