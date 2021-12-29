package pt.isec.amov.reversi.game

import android.graphics.Bitmap

class Player(playerNumber: Int, initialPieces: Int, playerColor: Int,user:String = "Diogo Almeida",nrBombs: Int=1,nrexchangePieces: Int=1) {

    private var pieces = initialPieces
    private var pieceType = playerNumber
    private var color = playerColor
    private var username = user
    private var bombPiece =nrBombs
    private var exchangePieces = nrexchangePieces
    private var photo : Bitmap? = null

    fun setPhoto(photo: Bitmap) {
        this.photo = photo
    }

    fun setBombPiece(){
        bombPiece--
    }

    fun setExchangePieces(){
        exchangePieces--
    }

    fun setPieces(pieces : Int){
        this.pieces = pieces
    }

    fun setUsername(username : String){
        this.username = username
    }

    fun getPhoto(): Bitmap? = photo

    fun getColor() : Int = color

    fun getPieceType() : Int = pieceType

    fun getPieces(): Int  = pieces

    fun getUsername() : String = username

    fun getBombPiece() : Int = bombPiece

    fun getExchangePieces():Int = exchangePieces
}