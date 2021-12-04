package pt.isec.amov.reversi.game


import kotlin.random.Random
import kotlin.random.nextInt

class BoardGame(private var gamemode: Int, private var colorsPlayers: ArrayList<Int>, private var colorsBoard: ArrayList<Int>) {

    companion object {
        const val EMPTY_CELL = 0
    }

    private var boardSIZE = getBoardSize()
    private var pieces = Array(boardSIZE) { IntArray(boardSIZE) }

    private var currentPlayer = 0
    private var validPlays = ArrayList<PieceMoves>()
    private val players = arrayListOf<Player>()

    init {
        newGame()
    }

    private fun newGame() {
        for (i in 0 until boardSIZE)
            for (j in 0 until boardSIZE)
                pieces[i][j] = EMPTY_CELL

        players.clear()
        val middle = boardSIZE / 2

        if (gamemode != 2) {

            for (i in 1..2)
                players.add(Player(i, 2, colorsPlayers[i - 1]))


            pieces[middle - 1][middle - 1] = players[0].getPieceType()
            pieces[middle][middle] = players[0].getPieceType()
            pieces[middle][middle - 1] = players[1].getPieceType()
            pieces[middle - 1][middle] = players[1].getPieceType()

            currentPlayer = rafflePlayer(2)

        } else {
            for (i in 1..3)
                players.add(Player(i, 4, colorsPlayers[i - 1]))

            pieces[middle - 1][middle - 3] = players[0].getPieceType()
            pieces[middle][middle - 2] = players[0].getPieceType()
            pieces[middle - 3][middle + 2] = players[0].getPieceType()
            pieces[middle - 2][middle + 1] = players[0].getPieceType()

            pieces[middle - 1][middle - 2] = players[1].getPieceType()
            pieces[middle][middle - 3] = players[1].getPieceType()
            pieces[middle + 1][middle + 1] = players[1].getPieceType()
            pieces[middle + 2][middle + 2] = players[1].getPieceType()

            pieces[middle - 3][middle + 1] = players[2].getPieceType()
            pieces[middle - 2][middle + 2] = players[2].getPieceType()
            pieces[middle + 1][middle + 2] = players[2].getPieceType()
            pieces[middle + 2][middle + 1] = players[2].getPieceType()

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
        var checkX = 0
        var checkY = 0
        var befY = 0
        var befX = 0
        for (i in 0 until boardSIZE) {
            for (j in 0 until boardSIZE) {
                /* Verifica se a peça pertence a quem está a jogar */
                if (pieces[i][j] == getPieceType()) {
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            if (dx == 0 && dy == 0)
                                continue
                            else {
                                for (nextStep in 1 until boardSIZE) {   // para conseguir ir ate à ponta do tabuleiro
                                    if (nextStep > 1) { //apartir da segunda vez
                                        befX = checkX      // guardar o x da peca anterior
                                        befY = checkY      // guardar o y da peca anterior
                                    }
                                    //
                                    checkX = i + (dx * nextStep)
                                    checkY = j + (dy * nextStep)

                                    // verificar tamanho do tabuleiro & verificar celula é do jogador
                                    if (checkX < 0 || checkY < 0 || checkX >= boardSIZE || checkY >= boardSIZE || pieces[checkX][checkY] == getPieceType())
                                        break

                                    // se celula vazia no raio de 1 => avançar
                                    if (pieces[checkX][checkY] == EMPTY_CELL && nextStep == 1)
                                        break

                                    if (nextStep > 1)
                                        if (pieces[checkX][checkY] == EMPTY_CELL && pieces[befX][befY] != getPieceType()) {
                                            possiblePlays.add(PieceMoves(checkX, checkY))
                                            break
                                        }

                                }
                            }
                        }
                    }
                }
            }
        }
        validPlays = possiblePlays
        return possiblePlays
    }

    fun confirmMove(x: Int, y: Int): Boolean {
        for (i in 0 until validPlays.size) {
            if (x == validPlays[i].getX() && y == validPlays[i].getY())
                return true
        }
        return false
    }

    fun move(posX: Int, posY: Int): Int {
        // Verificar se está dentro das margens
        if (posX < 0 || posY < 0 || posX >= boardSIZE || posY >= boardSIZE) {
            return -1
        }

        // Verificar se a cell já está ocupada
        if (pieces[posX][posY] != EMPTY_CELL)
            return -1

        var nPiecesCaptured = 0
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0)
                    continue
                for (nextStep in 1 until boardSIZE) {
                    val checkX = posX + (dx * nextStep)
                    val checkY = posY + (dy * nextStep)

                    if (checkX < 0 || checkY < 0 || checkX >= boardSIZE || checkY >= boardSIZE || pieces[checkX][checkY] == EMPTY_CELL)
                        break

                    // Encontrar uma peça do currentPlayer
                    if (pieces[checkX][checkY] == getPieceType()) {
                        // Verificar que está num raio maior que 1
                        if (nextStep > 1) {
                            nPiecesCaptured += nextStep - 1   // Adiciona peças capturadas
                            var aux = nextStep
                            while (aux-- > 0) {
                                pieces[posX + (dx * aux)][posY + (dy * aux)] =
                                    getPieceType()      // Atribuir as peças capturadas o novo tipo
                            }
                        }
                        break
                    }

                }
            }
        }
        return nPiecesCaptured
    }

    private fun checkEndGamePlays(): Boolean {
        var i = 0
        while (highlightValidPlays().size == 0) {
            if (getPlayers() - 1 == i)
                return true
            switchPlayer()
            i++
        }
        return false
    }

    private fun checkEndGameBoard(): Boolean {
        var count = 0
        for (i in 0 until players.size)
            count += players[i].getPieces()

        if (count >= boardSIZE * boardSIZE)
            return true
        return false
    }

    fun checkWinner(): Player? {
        var winner: Player? = players[0]
        var count =  false

        for (i in 0 until players.size - 1) // 0, 1 em modo de 3     5 - 10 - 10
            for (j in (i + 1) until players.size) {   // 1, 2 em modo de 3
                if (players[i].getPieces() < players[j].getPieces() && winner?.getPieces()!! < players[j].getPieces()) {
                    winner = players[j]
                    count = false
                    break
                } else if (winner?.getPieces() == players[j].getPieces())
                    count = true
            }
        if (count)
            return null
        return winner
    }

    fun getBoardSize(): Int {
        if (gamemode != 2)
            return 8
        return 10
    }

    fun switchPlayer() {
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

    fun checkEndGame(): Boolean = checkEndGameBoard() || checkEndGamePlays()

    fun getCurrentPlayer(): Int  = currentPlayer

    fun getPlayers(): Int = players.size

    fun getBoardColor(number : Int): Int = colorsBoard[number]

    fun getPiece(x: Int, y: Int): Int = pieces[x][y]

    private fun getPieceType(): Int = players[currentPlayer - 1].getPieceType()

    fun getColor(playerNumber: Int): Int = players[playerNumber].getColor()

    fun getGameMode() :Int = gamemode

    fun getUsername(number: Int) : String{
        return players[number].getUsername()
    }

    private fun rafflePlayer(nPlayers: Int): Int = Random.nextInt(1..nPlayers)
}