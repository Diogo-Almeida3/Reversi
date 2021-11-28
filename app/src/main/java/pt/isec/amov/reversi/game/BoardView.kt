package pt.isec.amov.reversi.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import pt.isec.amov.reversi.activities.GameActivity

private const val LINE_SIZE = 4
private const val MARGIN = 6
class BoardView : View {



    private var gamemode = 0
    private var windowHeight = 0;
    private var windowWidth = 0
    private var stoneHeight = 0;
    private var stoneWidth = 0

    private lateinit var gameActivity: GameActivity
    private lateinit var boardGame: BoardGame

    private var BOARD_SIZE = 0
    private val gridPaint = Paint(Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 4.0f
        style = Paint.Style.FILL_AND_STROKE
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    fun setData(gameActivity: GameActivity, gamemode: Int, boardGame: BoardGame) {
        this.gameActivity = gameActivity
        this.gamemode = gamemode
        this.boardGame = boardGame
        BOARD_SIZE = getBoardSize()
    }

    private fun getBoardSize(): Int {
        if (gamemode != 2)
            return 8
        return 10
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* Tamanho da janela */
        windowHeight = MeasureSpec.getSize(heightMeasureSpec)
        windowWidth = MeasureSpec.getSize(widthMeasureSpec)

        /* Tamanho das peças */
        stoneHeight = (windowHeight - LINE_SIZE) / BOARD_SIZE
        stoneWidth = (windowWidth - LINE_SIZE) / BOARD_SIZE

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun drawBoard(canvas: Canvas?) {
        for (i in 0..BOARD_SIZE - 1)
            for (j in 0..BOARD_SIZE - 1){
                val pieceType = boardGame.getPiece(i, j)
                if (pieceType != 0)
                    drawPiece(canvas, i, j,pieceType)
            }

    }

    private fun drawGrid(canvas: Canvas?) {
        for (i in -1..BOARD_SIZE) {
            /* Horizontal */
            var low = stoneHeight * (i + 1)
            canvas?.drawRect(
                0f,
                low.toFloat(), width.toFloat(), (low + LINE_SIZE).toFloat(), gridPaint
            )

            /* Vertical */
            low = stoneWidth * (i + 1)
            canvas?.drawRect(
                low.toFloat(), 0f, ((low + LINE_SIZE).toFloat()),
                height.toFloat(), gridPaint
            )
        }

    }

    override fun onDraw(canvas: Canvas?) {
        drawGrid(canvas) //Constrói o grid
        drawBoard(canvas) // Constrói as peças iniciais
    }

    private fun drawPiece(canvas: Canvas?, x: Int, y: Int, pieceType: Int) {

        val centerX = (stoneWidth * x) + stoneWidth / 2
        val centerY = (stoneHeight * y) + stoneHeight / 2

        val radius = Math.min(stoneWidth, stoneHeight) / 2 - MARGIN * 2
        val paint = Paint().apply { color = Color.WHITE }

        if (gamemode != 2) {
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