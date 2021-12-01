package pt.isec.amov.reversi.game


import kotlin.random.Random
import kotlin.random.nextInt

class BoardGame(private var gamemode: Int, private var colors: ArrayList<Int>) {

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
        for (i in 0 until BOARD_SIZE)
            for (j in 0 until BOARD_SIZE)
                pieces[i][j] = EMPTY_CELL

        player.clear()
        val middle = BOARD_SIZE / 2

        if (gamemode != 2) {

            for (i in 1..2)
                player.add(Player(i, 2, colors[i - 1]))


            pieces[middle - 1][middle - 1] = player[0].getPieceType()
            pieces[middle][middle] = player[0].getPieceType()
            pieces[middle][middle - 1] = player[1].getPieceType()
            pieces[middle - 1][middle] = player[1].getPieceType()

            currentPlayer = rafflePlayer(2)

        } else {
            for (i in 1..3)
                player.add(Player(i, 4, colors[i - 1]))

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

    fun highlightValidPlays(): ArrayList<PieceMoves> {
        /*
        * Não é necessário preocupar onde o utilizador clica pois só estamos a fazer o highlight
        *
        * Vamos correr todas as posições do tabuleiro -> ciclo for dentro de ciclo for
        *
        * Cada vez que encontrar-mos uma cell vazia passamos para a seguinte
        *
        * Quando encontra-mos uma peça Vermelha vamos fazer uma verificação num raio de 1 celula
        * Vemos se for vazia ou da mesma cor passa a frente se for de outra cor vai seguir essa direção até encontrar uma celula branca
        * ou uma cell da sua cor
        *
        * No primeiro caso se for cell vazia pode jogar la
        * No segundo caso se encontrar cell da mesma cor esse movimento é proibido
        * No caso de encontrar cell doutra cor continua a seguir o caminho
        */
        val possiblePlays = ArrayList<PieceMoves>()
        var check_x = 0
        var check_y = 0
        var befY = 0
        var befX = 0
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                /* Verifica se a peça pertence a quem está a jogar */
                if (pieces[i][j] == getPieceType()){
                    for(dx in -1..1){
                        for(dy in -1..1){
                            if(dx == 0 && dy == 0)
                                continue
                            else{
                                for(nextStep in 1 until BOARD_SIZE){   // para conseguir ir ate à ponta do tabuleiro
                                    if(nextStep > 1){ //apartir da segunda vez
                                        befX = check_x      // guardar o x da peca anterior
                                        befY = check_y      // guardar o y da peca anterior
                                    }
                                    //
                                    check_x = i + (dx * nextStep)
                                    check_y = j + (dy * nextStep)

                                    // verificar tamanho do tabuleiro & verificar celula é do jogador
                                    if(check_x < 0 || check_y < 0 || check_x >= BOARD_SIZE || check_y >= BOARD_SIZE || pieces[check_x][check_y] == getPieceType() )
                                        break

                                    // se celula vazia no raio de 1 => avançar
                                    if(pieces[check_x][check_y] == EMPTY_CELL && nextStep ==1)
                                        break;

                                    if(nextStep > 1)
                                        if(pieces[check_x][check_y] == EMPTY_CELL && pieces[befX][befY]  != getPieceType()){
                                            possiblePlays.add(PieceMoves(check_x,check_y))
                                            break;
                                        }

                                }
                            }
                        }
                    }
                }
            }
        }
        return possiblePlays
    }


    fun getPiece(x: Int, y: Int): Int {
        return pieces[x][y]
    }

    fun getPieceType(): Int {
        return player[currentPlayer - 1].getPieceType()
    }

    fun getColor(playerNumber: Int): Int {
        return player[playerNumber].getColor()
    }

    private fun switchPlayer() {
        if (gamemode != 2) {
            when (currentPlayer) {
                1 -> currentPlayer = 2
                2 -> currentPlayer = 1
            }
        } else {
            when (currentPlayer) {
                1 -> currentPlayer = 2
                2 -> currentPlayer = 3
                3 -> currentPlayer = 1
            }
        }
    }

    private fun rafflePlayer(nPlayers: Int): Int {
        return Random.nextInt(1..nPlayers)
    }

    private fun getBoardSize(): Int {
        if (gamemode != 2)
            return 8
        return 10
    }
}