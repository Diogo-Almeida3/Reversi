package pt.isec.amov.reversi.game

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.navigation.findNavController
import pt.isec.amov.reversi.R


private const val LINE_SIZE = 5
private const val MARGIN_PIECE = 8
private const val MARGIN_HIGHLIGHT = 32

class BoardView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var windowHeight = 0
    private var windowWidth = 0
    private var pieceHeight = 0
    private var pieceWidth = 0
    private var endGame = false
    private lateinit var boardGame: BoardGame
    private lateinit var gamePerfilView: GamePerfilView

    private var counter = 0

    companion object{
        const val NORMAL_PIECE = 0
        const val BOMB_PIECE = 1
        const val EXCHANGE_PIECE = 2
    }

    private var exchangeArrayList = ArrayList<PieceMoves>()
    private var exchangeCounter = 0
    private var boardSIZE = 0

    fun setData(boardGame: BoardGame, gamePerfilView: GamePerfilView) {
        this.boardGame = boardGame
        this.gamePerfilView = gamePerfilView
        boardSIZE = boardGame.getBoardSize()
        exchangeCounter = 0
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* Tamanho da janela */
        windowHeight = MeasureSpec.getSize(heightMeasureSpec)
        windowWidth = MeasureSpec.getSize(widthMeasureSpec)

        /* Tamanho das peças */
        pieceHeight = (windowHeight - LINE_SIZE) / boardSIZE
        pieceWidth = (windowWidth - LINE_SIZE) / boardSIZE

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun drawBoard(canvas: Canvas?) {
        for (i in 0 until boardSIZE)
            for (j in 0 until boardSIZE) {
                val pieceType = boardGame.getPiece(i, j)
                if (pieceType != 0)
                    drawPiece(canvas, i, j, pieceType)
            }

    }

    private fun drawGrid(canvas: Canvas?) {
        val gridPaint = getGridPaint()

        drawCellColor(canvas)
        for (i in -1..boardSIZE) {
            /* Horizontal */
            var low = pieceHeight * (i + 1)
            canvas?.drawRect(0f, low.toFloat(), width.toFloat() - MARGIN_PIECE, (low + LINE_SIZE).toFloat(), gridPaint)

            /* Vertical */
            low = pieceWidth * (i + 1)
            canvas?.drawRect(low.toFloat(), 0f, ((low + LINE_SIZE).toFloat()), height.toFloat() - MARGIN_PIECE, gridPaint)
        }
    }

    private fun drawCellColor(canvas: Canvas?) {
        for (i in 0 until boardSIZE) {
            for (j in 0 until boardSIZE) {
                val left = (pieceWidth * i)
                val top = (pieceHeight * j)
                val right = (pieceWidth * i) + pieceWidth
                val bottom = (pieceHeight * j) + pieceHeight
                canvas?.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), getCellPaint())
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {

        drawGrid(canvas) //Constrói o grid
        drawBoard(canvas) // Constrói as peças iniciais
        drawHighlightValidPlays(canvas, boardGame.highlightValidPlays()) // constroi possiveis jogadas

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = (event?.x?.div(pieceWidth))?.toInt()
        val y = (event?.y?.div(pieceHeight))?.toInt()
        counter = 0
        if (!endGame) {
            when (boardGame.getGameMode()) {
                0 -> {
                    when(boardGame.getCurrentPiece()){
                        NORMAL_PIECE -> {
                            if (boardGame.confirmMove(x!!, y!!)) {
                                boardGame.move(x, y)
                                boardGame.checkBoardPieces()

                                if (boardGame.checkEndGame())
                                    alertEndGame("Acabou o jogo. Tabuleiro Preenchido")
                                else
                                    checkAlertNoPlays()
                            }
                        }

                        BOMB_PIECE -> {
                            if(boardGame.confirmBombMove(x!!,y!!)){
                                boardGame.pieceBomb(x,y)
                                boardGame.checkBoardPieces()
                                checkAlertNoPlays()
                            } else
                                showAlertExchange("You can only select your piece color")
                        }

                        EXCHANGE_PIECE -> {
                            when(boardGame.confirmExchangeMove(x!!,y!!,exchangeCounter,exchangeArrayList)){
                                -3 -> showAlertExchange("You can't choose the same pieces twice")
                                -2 -> showAlertExchange("Wrong Piece selected")
                                -1 -> showAlertExchange("You can't choose a position outside the board")
                                1 -> {
                                    exchangeArrayList.add(PieceMoves(x,y))
                                    exchangeCounter++

                                    if(exchangeCounter >= 3){
                                        boardGame.exchangePiece(exchangeArrayList)
                                        exchangeCounter = 0
                                        boardGame.checkBoardPieces()
                                        checkAlertNoPlays()
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun updateView() {
        gamePerfilView.invalidate()
        invalidate()
    }

    private fun alertEndGame(text:String){
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        showAlertEndGame(boardGame.checkWinner())
        updateView()
        endGame = true
    }

    private fun checkAlertNoPlays(): Boolean {
        //Muda o jogador a jogar
        boardGame.switchPlayer()

        //Verifica se ele tem jogadas disponiveis se sim saimos daqui e continuamos
        if (boardGame.checkNoValidPlays()){
            updateView()
            return true
        }

        if(counter == 0)
            updateView()

        //Senao mostra o alert e resolve o resto lá dentro
        showAlertPassPlay(boardGame.getName())
        return false

    }

    private fun showAlertEndGame(player: Player?) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)

        if(player != null)
            builder1.setMessage("${player.getUsername()} win the game.")
        else
            builder1.setMessage("Draw")
        builder1.setCancelable(false)

        builder1.setPositiveButton("Check Board") { dialog, id ->
            run {
                dialog.cancel()
            }
        }

        builder1.setNegativeButton("Back to menu") { dialog, id ->
            run {
                dialog.cancel()
                findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    private fun showAlertExchange(phrase: String) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        builder1.setMessage(phrase)
        builder1.setCancelable(false)

        builder1.setPositiveButton("Pass") { dialog, id ->
            run {
                dialog.cancel()
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    private fun showAlertPassPlay(name: String) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        builder1.setMessage(name + " has no available plays..")
        builder1.setCancelable(false)

        builder1.setPositiveButton("Pass") { dialog, id ->
            run {
                dialog.cancel()
                ++counter
                if (counter < boardGame.getPlayers()) //Quando for igual já sabemos que percorreu os jogadores todos portanto não vale apena fazer o igual
                    checkAlertNoPlays()
                else
                    alertEndGame("Acabou o jogo. Não existem jogadas disponiveis")
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    private fun drawHighlightValidPlays(canvas: Canvas?, highlightValidPlays: ArrayList<PieceMoves>) {
        for (i in 0 until highlightValidPlays.size) {
            val centerX = (pieceWidth * highlightValidPlays[i].getX()) + pieceWidth / 2
            val centerY = (pieceHeight * highlightValidPlays[i].getY()) + pieceHeight / 2
            val radius = Math.min(pieceWidth, pieceHeight) / 2 - MARGIN_HIGHLIGHT
            canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), radius.toFloat(), getHighlightPlayPaint())
            canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), radius.toFloat(), getHighlightPlayStrokePaint())
        }
    }


    private fun getGridPaint() : Paint{
        return Paint(Paint.DITHER_FLAG and Paint.ANTI_ALIAS_FLAG).apply {
            color = boardGame.getBoardColor(1)
        }
    }

    private fun getCellPaint() : Paint{
        return Paint().apply { color = boardGame.getBoardColor(0) }
    }

    private fun getHighlightPlayStrokePaint() : Paint{
        return Paint(Paint.ANTI_ALIAS_FLAG and Paint.DITHER_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6.0f
        }
    }

    private fun getHighlightPlayPaint() : Paint{
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = boardGame.getBoardColor(2)
        }
    }

    private fun drawPiece(canvas: Canvas?, x: Int, y: Int, pieceType: Int) {

        val centerX = (pieceWidth * x) + pieceWidth / 2
        val centerY = (pieceHeight * y) + pieceHeight / 2

        val radius = Math.min(pieceWidth, pieceHeight) / 2 - MARGIN_PIECE * 2
        val paint = Paint().apply { color = Color.WHITE }

        if (boardGame.getGameMode() != 2) {
            when (pieceType) {
                1 -> paint.color = boardGame.getColor(0)
                2 -> paint.color = boardGame.getColor(1)
            }
        } else {
            when (pieceType) {
                1 -> paint.color = boardGame.getColor(0)
                2 -> paint.color = boardGame.getColor(1)
                3 -> paint.color = boardGame.getColor(2)
            }
        }

        canvas?.drawCircle(
            centerX.toFloat(), centerY.toFloat(),
            radius.toFloat(), paint
        )
    }

    
}