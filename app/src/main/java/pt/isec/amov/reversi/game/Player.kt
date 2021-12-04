package pt.isec.amov.reversi.game

class Player(playerNumber: Int, initialPieces: Int, playerColor: Int,user:String = "Diogo Almeida") {

    private var pieces = initialPieces
    private var pieceType = playerNumber
    private var color = playerColor
    private var username = user

    fun getColor() : Int = color

    fun getPieceType() : Int = pieceType

    fun getPieces(): Int  = pieces

    fun getUsername() : String = username

    fun setPieces(pieces : Int){
        this.pieces = pieces
    }
}