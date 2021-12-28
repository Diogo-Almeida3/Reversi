package pt.isec.amov.reversi.game

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.firebase.ktx.Firebase
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.fragments.GameFragment


private const val LINE_SIZE = 5
private const val MARGIN_PIECE = 8
private const val MARGIN_HIGHLIGHT = 32

const val SERVER_PORT = 9999


class BoardView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    companion object {
        const val NORMAL_PIECE = 0
        const val BOMB_PIECE = 1
        const val EXCHANGE_PIECE = 2
    }

    private var windowHeight = 0
    private var windowWidth = 0
    private var pieceHeight = 0
    private var pieceWidth = 0
    private var boardSIZE = 0



    private lateinit var boardGame: BoardGame
    private  lateinit var gamePerfilView: GamePerfilView
    private lateinit var gameFragment: GameFragment

    //fun getexchangeError() : Int = exchangeError

    fun setData(boardGame: BoardGame, gameProfileView: GamePerfilView,gameFragment: GameFragment) {
        this.boardGame = boardGame
        this.gamePerfilView = gameProfileView
        this.gameFragment = gameFragment
        boardSIZE = boardGame.getBoardSize()
        //exchangeCounter = 0
       // exchangeError = 0

        /* Multiplayer */
        //validPlay = false
        //state.postValue(State.SETTING_PROFILE_DATA)
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //if(gameFragment.getGameState() != GameFragment.State.GAME_OVER){
            //gameFragment.cleanUp()
        //}

        //Quando eu sair e der pop do fragmento anterior o garbage collector elimina as ligações
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = (event?.x?.div(pieceWidth))?.toInt()
        val y = (event?.y?.div(pieceHeight))?.toInt()
        gameFragment.resetCounter()
        when (boardGame.getGameMode()) {
            0 -> {
                if (!gameFragment.getEndgame()) {
                    gameFragment.movePiece(x!!,y!!,boardGame.getCurrentPiece())
                }
            }
            1 -> {
                gameFragment.check2OnlineMove(x,y)
            }
        }


        return super.onTouchEvent(event)
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
                low.toFloat(),
                0f,
                ((low + LINE_SIZE).toFloat()),
                height.toFloat() - MARGIN_PIECE,
                gridPaint
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
                canvas?.drawRect(
                    left.toFloat(),
                    top.toFloat(),
                    right.toFloat(),
                    bottom.toFloat(),
                    getCellPaint()
                )
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

    private fun updateView() {
        gamePerfilView.invalidate()
        invalidate()
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
                centerX.toFloat(),
                centerY.toFloat(),
                radius.toFloat(),
                getHighlightPlayPaint()
            )
            canvas?.drawCircle(
                centerX.toFloat(),
                centerY.toFloat(),
                radius.toFloat(),
                getHighlightPlayStrokePaint()
            )
        }
    }

    private fun getGridPaint(): Paint {
        return Paint(Paint.DITHER_FLAG and Paint.ANTI_ALIAS_FLAG).apply {
            color = boardGame.getBoardColor(1)
        }
    }

    private fun getCellPaint(): Paint {
        return Paint().apply { color = boardGame.getBoardColor(0) }
    }

    private fun getHighlightPlayStrokePaint(): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG and Paint.DITHER_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6.0f
        }
    }

    private fun getHighlightPlayPaint(): Paint {
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