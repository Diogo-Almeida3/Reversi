package pt.isec.amov.reversi.game

class Player(playerNumber: Int, initialPieces: Int, playerColor: Int,user:String = "Diogo Almeida",nrBombs: Int=1,nrexchangePieces: Int=1) {

    private var pieces = initialPieces
    private var pieceType = playerNumber
    private var color = playerColor
    private var username = user
    private var bombPiece =nrBombs
    private var exchangePieces = nrexchangePieces

    fun getColor() : Int = color

    fun getPieceType() : Int = pieceType

    fun getPieces(): Int  = pieces

    fun getUsername() : String = username

    fun getBombPiece() : Int = bombPiece

    fun getExchangePieces():Int = exchangePieces

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
}