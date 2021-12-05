package pt.isec.amov.reversi.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import pt.isec.amov.reversi.R
import android.graphics.BitmapFactory


import androidx.core.graphics.scale


private const val MARGIN = 24
private const val LINE_SIZE = 5
private const val MARGIN_PIECE = 8

class GamePerfilView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var windowHeight = 0
    private var windowWidth = 0
    private var pieceHeight = 0
    private var pieceWidth = 0
    private lateinit var boardGame: BoardGame

    fun setData(boardGame: BoardGame) {
        this.boardGame = boardGame

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* Tamanho da janela */
        windowHeight = MeasureSpec.getSize(heightMeasureSpec)
        windowWidth = MeasureSpec.getSize(widthMeasureSpec)

        pieceHeight = (windowHeight - LINE_SIZE) / boardGame.getBoardSize()
        pieceWidth = (windowWidth - LINE_SIZE) / boardGame.getBoardSize()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        drawBackground(canvas)
        drawData(canvas)
    }

    private fun drawBackground(canvas: Canvas?) {
        for (i in 0 until boardGame.getPlayers()) {
            val left = (windowWidth / boardGame.getPlayers()) * i + MARGIN
            val top = 0
            val right = (windowWidth /boardGame.getPlayers()) * i + (windowWidth / boardGame.getPlayers()) - MARGIN
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
                    color = boardGame.getBoardColor(1)
                    style = Paint.Style.STROKE
                    strokeWidth = 10.0f
                })
        }
    }

    private fun getNameSize() : Float{ //Limitar o nome a 15 chars
        if(boardGame.getGameMode() != 2)
            return 56F
        return 38F
    }

    private fun getScoreSize() : Float {
        if(boardGame.getGameMode() != 2)
            return 82F
        return 56F
    }

    private fun drawData(canvas: Canvas?) {
        for(i in 0 until boardGame.getPlayers()){
            val right = (windowWidth /boardGame.getPlayers()) * i + (windowWidth / boardGame.getPlayers()) - MARGIN
            val left = (windowWidth / boardGame.getPlayers()) * i + MARGIN
            /* Image Related */
            val imageSize = 400 / boardGame.getPlayers()
            val middle = right - ((right-left)/2)
            val imagePos = middle - (imageSize/2)
            val size = getNameSize()

            /* Text Related */
            val paintName = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = size
            }
            val nChars = boardGame.getUsername(i).length

            /* Score Related */
            val boxQuarter = (right - left) / 4
            val centerX = middle + boxQuarter
            val centerY = imageSize + 240
            val paint = Paint().apply { color = boardGame.getColor(i) }
            val textPosX = middle - boxQuarter
            val textPosY = imageSize + 275
            val paintScore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = getScoreSize()
            }
            val actualScore = boardGame.getTotalPieces(i)

            when(boardGame.getGameMode()){
                0 -> {
                    if(i == 0){ //Jogador atual
                        canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize,imageSize,false),imagePos.toFloat(),50f,null)
                        canvas?.drawText(boardGame.getUsername(i),0,nChars, (middle - nChars*15).toFloat(),(imageSize + 120).toFloat(),paintName)
                    }
                    else{ //Anónimo
                        canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher).scale(imageSize,imageSize,false),imagePos.toFloat(),50f,null)
                        canvas?.drawText("Anónimo",0,7, (middle - 7*15).toFloat(),(imageSize + 120).toFloat(),paintName)
                    }
                    canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), 65f, paint)
                }
                1 -> {
                    canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize,imageSize,false),imagePos.toFloat(),50f,null)
                    canvas?.drawText(boardGame.getUsername(i),0,nChars, (middle - nChars*15).toFloat(),(imageSize + 120).toFloat(),paintName)
                    canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), 65f, paint)
                }
                2 -> {
                    canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize,imageSize,false),imagePos.toFloat(),50f,null)
                    canvas?.drawText(boardGame.getUsername(i),0,nChars, (middle - nChars*8.75).toFloat(),(imageSize + 120).toFloat(),paintName)
                    canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), 50f, paint)
                }
            }

            canvas?.drawText(actualScore.toString(), textPosX.toFloat(),textPosY.toFloat(),paintScore)
        }
    }

}