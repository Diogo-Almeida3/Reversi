package pt.isec.amov.reversi.game

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.activities.GameActivity
import java.lang.String
import kotlin.random.Random

class BoardGame(private var gamemode: Int) {

    companion object {
        const val EMPTY_CELL = 0
    }

    private var BOARD_SIZE = getBoardSize()
    private var pieces = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) }

    private val player = ArrayList<Player>()
    private var currentPlayer = 0

    init {
        newGame()
    }

    private fun newGame() {
        for (i in 0..BOARD_SIZE - 1)
            for (j in 0..BOARD_SIZE - 1)
                pieces[i][j] = EMPTY_CELL

        player.clear()
        val middle = BOARD_SIZE / 2


        val colors = ArrayList<Int>(3)
        colors.add(Color.RED)
        colors.add(Color.GREEN)
        colors.add(Color.BLUE)

        if (gamemode != 2) {

            for(i in 1..2)
                player.add(Player(i,2,colors[i-1]))


            pieces[middle - 1][middle - 1] = player[0].getPieceType()
            pieces[middle][middle] = player[0].getPieceType()
            pieces[middle][middle - 1] = player[1].getPieceType()
            pieces[middle - 1][middle] = player[1].getPieceType()

            currentPlayer = rafflePlayer(2)

        } else {
            for(i in 1..3)
                player.add(Player(i,4, colors[i-1]))

            pieces[middle - 1][middle - 3] = player[0].getPieceType()
            pieces[middle][middle - 2] = player[0].getPieceType()
            pieces[middle - 3][middle + 2] = player[0].getPieceType()
            pieces[middle - 2][middle + 1] = player[0].getPieceType()

            pieces[middle - 1][middle - 2] = player[1].getPieceType()
            pieces[middle][middle - 3] = player[1].getPieceType()
            pieces[middle + 1][middle + 1] = player[1].getPieceType()
            pieces[middle + 2][middle + 2] = player[1].getPieceType()

            pieces[middle - 3][middle + 1] = player[2].getPieceType()
            pieces[middle - 2][middle + 2] = player[2].getPieceType()
            pieces[middle + 1][middle + 2] = player[2].getPieceType()
            pieces[middle + 2][middle + 1] = player[2].getPieceType()

            currentPlayer = rafflePlayer(3)
        }
    }

    fun getPiece(x : Int, y : Int) : Int{
        return pieces[x][y]
    }

    fun getPieceType() : Int{
        return player[currentPlayer - 1].getPieceType()
    }

    fun getColor(playerNumber: Int) : Int {
            return player[playerNumber].getColor()
    }

    private fun switchPlayer(){
        if(gamemode != 2){
            when(currentPlayer){
                1 -> currentPlayer = 2
                2 -> currentPlayer = 1
            }
        } else{
            when(currentPlayer){
                1 -> currentPlayer = 2
                2 -> currentPlayer = 3
                3 -> currentPlayer = 1
            }
        }
    }

    private fun rafflePlayer(nPlayers : Int) : Int{
        return Random.nextInt(1, nPlayers)
    }

    private fun getBoardSize(): Int {
        if (gamemode != 2)
            return 8
        return 10
    }
}