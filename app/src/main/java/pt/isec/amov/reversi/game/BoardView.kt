package pt.isec.amov.reversi.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import pt.isec.amov.reversi.activities.GameActivity

private const val LINE_SIZE = 4

class BoardView : View {


    private var gamemode = 0
    private var windowHeight = 0; private var windowWidth = 0
    private var stoneHeight = 0; private var stoneWidth = 0

    private lateinit var gameActivity: GameActivity

    private fun getBoardSize(): Int {
        if (gamemode != 2)
            return 8
        return 10
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        windowHeight = MeasureSpec.getSize(heightMeasureSpec)
        windowWidth = MeasureSpec.getSize(widthMeasureSpec)
        stoneHeight = (windowHeight - LINE_SIZE) / getBoardSize()
        stoneWidth = (windowWidth - LINE_SIZE) / getBoardSize()

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun drawBoard(canvas: Canvas?) {

    }

    private fun drawGrid(canvas: Canvas?) {
        for (i in -1..getBoardSize()) {
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

    fun setData(gameActivity: GameActivity, gamemode: Int) {
        this.gameActivity = gameActivity
        this.gamemode = gamemode
    }

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onDraw(canvas: Canvas?) {
        drawGrid(canvas)
        drawBoard(canvas)
    }

    private val gridPaint = Paint(Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 4.0f
        style = Paint.Style.FILL_AND_STROKE
    }
}