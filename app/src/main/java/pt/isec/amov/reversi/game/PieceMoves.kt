package pt.isec.amov.reversi.game

class PieceMoves(x : Int, y: Int) {

    private var posX = x;
    private var posY = y;

    fun getX(): Int {
        return posX
    }
    fun getY(): Int {
        return posY
    }
}