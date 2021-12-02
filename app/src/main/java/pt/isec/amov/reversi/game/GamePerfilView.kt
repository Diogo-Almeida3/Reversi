package pt.isec.amov.reversi.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


private const val MARGIN = 24

class GamePerfilView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var windowHeight = 0
    private var windowWidth = 0

    private lateinit var boardGame: BoardGame
    private var nPlayers = 0

    fun setData(boardGame: BoardGame) {
        this.boardGame = boardGame
        nPlayers = boardGame.getPlayers()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* Tamanho da janela */
        windowHeight = MeasureSpec.getSize(heightMeasureSpec)
        windowWidth = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        drawBackground(canvas)
        //drawProfileData(canvas) //todo Iluminar ou colocar uma bola no jogador que é a jogar de modo a identificar
        //drawPonctuation(canvas)
    }

    private fun drawBackground(canvas: Canvas?) {
        for (i in 0 until nPlayers) {
            val left = (windowWidth / nPlayers) * i + MARGIN
            val top = 0
            val right = (windowWidth / nPlayers) * i + (windowWidth / nPlayers) - MARGIN
            val bottom = windowHeight
            val rect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

            canvas?.drawRoundRect(
                rect,
                20F,
                20F,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
            canvas?.drawRoundRect(
                rect,
                20F,
                20F,
                Paint(Paint.ANTI_ALIAS_FLAG and Paint.DITHER_FLAG).apply {
                    color = Color.BLUE
                    style = Paint.Style.STROKE
                    strokeWidth = 10.0f
                })
        }


    }

    private fun drawProfileData(canvas: Canvas?) {

    }

    private fun drawPonctuation(canvas: Canvas?) {

    }


}