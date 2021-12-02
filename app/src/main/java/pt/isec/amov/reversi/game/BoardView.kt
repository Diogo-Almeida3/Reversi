package pt.isec.amov.reversi.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

private const val LINE_SIZE = 5
private const val MARGIN_PIECE = 8
private const val MARGIN_HIGHLIGHT = 22

class BoardView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var windowHeight = 0
    private var windowWidth = 0
    private var pieceHeight = 0
    private var pieceWidth = 0

    private lateinit var boardGame: BoardGame

    private var BOARD_SIZE = 0
    private val gridPaint = Paint(Paint.DITHER_FLAG and Paint.ANTI_ALIAS_FLAG)

    fun setData(boardGame: BoardGame) {
        this.boardGame = boardGame
        BOARD_SIZE = boardGame.getBoardSize()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* Tamanho da janela */
        windowHeight = MeasureSpec.getSize(heightMeasureSpec)
        windowWidth = MeasureSpec.getSize(widthMeasureSpec)

        /* Tamanho das peças */
        pieceHeight = (windowHeight - LINE_SIZE) / BOARD_SIZE
        pieceWidth = (windowWidth - LINE_SIZE) / BOARD_SIZE

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun drawBoard(canvas: Canvas?) {
        for (i in 0 until BOARD_SIZE)
            for (j in 0 until BOARD_SIZE) {
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
        for (i in -1..BOARD_SIZE) {
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
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                val Left = (pieceWidth * i)
                val Top = (pieceHeight * j)
                val Right = (pieceWidth * i) + pieceWidth
                val Bottom = (pieceHeight * j) + pieceHeight
                canvas?.drawRect(Left.toFloat(),
                    Top.toFloat(),
                    Right.toFloat(),
                    Bottom.toFloat(),
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

        val x = (event?.getX()?.div(pieceWidth))?.toInt()
        val y = (event?.getY()?.div(pieceHeight))?.toInt()

        if (boardGame.getGameMode() == 0) {

            if (boardGame.confirmMove(x!!, y!!)) {
                boardGame.move(x, y)
                boardGame.switchPlayer()
            }

            boardGame.checkEndGame()
            invalidate()

        }
        return super.onTouchEvent(event)
    }

    private fun drawHighlightValidPlays(
        canvas: Canvas?,
        highlightValidPlays: ArrayList<PieceMoves>
    ) {
        for (i in 0 until highlightValidPlays.size) {
            val centerX = (pieceWidth * highlightValidPlays[i].getX()) + pieceWidth / 2
            val centerY = (pieceHeight * highlightValidPlays[i].getY()) + pieceHeight / 2
            val radius = Math.min(pieceWidth, pieceHeight) / 2 - MARGIN_HIGHLIGHT * 2
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

        if (canvas != null) {
            canvas.drawCircle(
                centerX.toFloat(), centerY.toFloat(),
                radius.toFloat(), paint
            )
        }

    }
}