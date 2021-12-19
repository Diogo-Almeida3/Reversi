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
    private var flagValidPlays = false
    private var boardSIZE = 0
    private val gridPaint = Paint(Paint.DITHER_FLAG and Paint.ANTI_ALIAS_FLAG)

    fun setData(boardGame: BoardGame, gamePerfilView: GamePerfilView) {
        this.boardGame = boardGame
        this.gamePerfilView = gamePerfilView
        boardSIZE = boardGame.getBoardSize()
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
        gridPaint.apply {
            color = boardGame.getBoardColor(1)
        }

        drawCellColor(canvas)
        for (i in -1..boardSIZE) {
            /* Horizontal */
            var low = pieceHeight * (i + 1)
            canvas?.drawRect(
                0f,
                low.toFloat(),
                width.toFloat() - MARGIN_PIECE,
                (low + LINE_SIZE).toFloat(),
                gridPaint
            )

            /* Vertical */
            low = pieceWidth * (i + 1)
            canvas?.drawRect(
                low.toFloat(), 0f, ((low + LINE_SIZE).toFloat()),
                height.toFloat() - MARGIN_PIECE, gridPaint
            )
        }
    }

    private fun drawCellColor(canvas: Canvas?) {
        for (i in 0 until boardSIZE) {
            for (j in 0 until boardSIZE) {
                val left = (pieceWidth * i)
                val top = (pieceHeight * j)
                val right = (pieceWidth * i) + pieceWidth
                val bottom = (pieceHeight * j) + pieceHeight
                canvas?.drawRect(left.toFloat(),
                    top.toFloat(),
                    right.toFloat(),
                    bottom.toFloat(),
                    Paint().apply { color = boardGame.getBoardColor(0) })
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {

        drawGrid(canvas) //Constrói o grid
        drawBoard(canvas) // Constrói as peças iniciais
        drawHighlightValidPlays(
            canvas,
            boardGame.highlightValidPlays()
        ) // constroi possiveis jogadas

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = (event?.x?.div(pieceWidth))?.toInt()
        val y = (event?.y?.div(pieceHeight))?.toInt()
        counter = 0
        if (!endGame) {
            when (boardGame.getGameMode()) {
                0 -> {
                    if (boardGame.confirmMove(x!!, y!!)) {
                        boardGame.move(x, y)
                        flagValidPlays = checkAlertNoPlays()
                    }
                    if (flagValidPlays)
                        updateView()
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun updateView() {
        boardGame.checkBoardPieces()
        if (boardGame.checkEndGame()) {
            Toast.makeText(
                context,
                "Acabou o jogo. Tabuleiro Preenchido",
                Toast.LENGTH_LONG
            ).show()
            endGame = true
        }
        gamePerfilView.invalidate()
        invalidate()
    }

    private fun checkAlertNoPlays(): Boolean {
        //Muda o jogador a jogar
        boardGame.switchPlayer()

        //Verifica se ele tem jogadas disponiveis se sim saimos daqui e continuamos
        if (boardGame.checkNoValidPlays())
            return true

        //Senao mostra o alert e resolve o resto lá dentro
        showAlert(boardGame.getName())
        return false

    }

    private fun showAlert(name: String) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        builder1.setMessage(name + " has no available plays..")
        builder1.setCancelable(true)

        builder1.setPositiveButton("Pass") { dialog, id ->
            run {
                dialog.cancel()
                if (counter != boardGame.getPlayers() - 1) {
                    ++counter
                    flagValidPlays = checkAlertNoPlays()

                    if (flagValidPlays)
                        updateView()
                    else {
                        if (counter == boardGame.getPlayers() - 1) {
                            Toast.makeText(
                                context,
                                "Acabou o jogo. Não existem jogadas disponiveis",
                                Toast.LENGTH_LONG
                            ).show()
                            endGame = true

                        }
                    }
                } else
                    updateView()
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    private fun drawHighlightValidPlays(
        canvas: Canvas?,
        highlightValidPlays: ArrayList<PieceMoves>
    ) {
        for (i in 0 until highlightValidPlays.size) {
            val centerX = (pieceWidth * highlightValidPlays[i].getX()) + pieceWidth / 2
            val centerY = (pieceHeight * highlightValidPlays[i].getY()) + pieceHeight / 2
            val radius = Math.min(pieceWidth, pieceHeight) / 2 - MARGIN_HIGHLIGHT
            canvas?.drawCircle(
                centerX.toFloat(), centerY.toFloat(),
                radius.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = boardGame.getBoardColor(2)
                })
            canvas?.drawCircle(
                centerX.toFloat(), centerY.toFloat(),
                radius.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG and Paint.DITHER_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 6.0f
                })
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