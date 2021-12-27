package pt.isec.amov.reversi.game.jsonClasses.profile

import android.graphics.Bitmap
import com.google.gson.annotations.SerializedName
import pt.isec.amov.reversi.game.GamePerfilView
import java.util.ArrayList

class GamePerfilData {

    var type: String? = null
    var usernames: ArrayList<String>? = null
    var nClients: Int? = null
    var photos : ArrayList<String>? = null
    var currentPlayer: Int? = null
    constructor() : super() {}

    constructor(nClients: Int,usernames: ArrayList<String>,photos: ArrayList<String>,currentPlayer : Int): super() {
        this.type = "PROFILE_VIEW"
        this.nClients = nClients
        this.usernames = usernames
        this.photos = photos
        this.currentPlayer = currentPlayer
    }
}